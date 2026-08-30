terraform {
  required_providers {
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

variable "name_prefix" {
  type = string
}

variable "region" {
  type = string
}

variable "github_repo" {
  description = "owner/repo (예: sumi-03/EventFlow)"
  type        = string
}

variable "github_branch" {
  description = "배포를 트리거하는 브랜치"
  type        = string
  default     = "main"
}

variable "ecr_repository_arn" {
  type = string
}

variable "ecs_cluster_name" {
  type = string
}

variable "ecs_service_name" {
  type = string
}

variable "task_execution_role_arn" {
  type = string
}

variable "task_role_arn" {
  type = string
}

data "aws_caller_identity" "current" {}

# GitHub OIDC 발급자의 현재 인증서 지문
data "tls_certificate" "github" {
  url = "https://token.actions.githubusercontent.com/.well-known/openid-configuration"
}

# GitHub Actions를 AWS 신뢰 대상으로 등록 -> 장기 액세스 키가 필요 없어짐
resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.github.certificates[0].sha1_fingerprint]
}

# 지정한 repo의 지정한 브랜치에서 실행된 워크플로우만 이 역할을 맡을 수 있음
data "aws_iam_policy_document" "assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repo}:ref:refs/heads/${var.github_branch}"]
    }
  }
}

resource "aws_iam_role" "deploy" {
  name               = "${var.name_prefix}-gha-deploy"
  assume_role_policy = data.aws_iam_policy_document.assume.json
}

# 배포에 딱 필요한 액션만: ECR push, 태스크 정의 등록, 이 서비스 갱신, 이 두 역할 전달
data "aws_iam_policy_document" "deploy" {
  statement {
    sid       = "EcrAuthToken"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"] # 이 액션은 리소스 수준 제한을 지원하지 않음
  }

  statement {
    sid = "EcrPushPull"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
      "ecr:PutImage",
    ]
    resources = [var.ecr_repository_arn]
  }

  statement {
    sid       = "EcsRegisterTaskDef"
    actions   = ["ecs:RegisterTaskDefinition", "ecs:DescribeTaskDefinition"]
    resources = ["*"] # RegisterTaskDefinition은 리소스 ARN 제한 불가
  }

  statement {
    sid     = "EcsDeployService"
    actions = ["ecs:UpdateService", "ecs:DescribeServices"]
    resources = [
      "arn:aws:ecs:${var.region}:${data.aws_caller_identity.current.account_id}:service/${var.ecs_cluster_name}/${var.ecs_service_name}"
    ]
  }

  statement {
    sid       = "PassTaskRoles"
    actions   = ["iam:PassRole"]
    resources = [var.task_execution_role_arn, var.task_role_arn]
    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "deploy" {
  name   = "deploy"
  role   = aws_iam_role.deploy.id
  policy = data.aws_iam_policy_document.deploy.json
}

output "deploy_role_arn" {
  value = aws_iam_role.deploy.arn
}

output "oidc_provider_arn" {
  value = aws_iam_openid_connect_provider.github.arn
}
