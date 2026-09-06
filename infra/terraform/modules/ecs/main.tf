variable "name_prefix" {
  type = string
}

variable "region" {
  type = string
}

variable "subnet_ids" {
  description = "태스크를 놓을 private 서브넷 (아웃바운드는 NAT 경유)"
  type        = list(string)
}

variable "security_group_id" {
  description = "app SG"
  type        = string
}

variable "target_group_arn" {
  type = string
}

variable "image" {
  description = "전체 이미지 참조 (repo_url:tag)"
  type        = string
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "cpu" {
  type    = number
  default = 512
}

variable "memory" {
  type    = number
  default = 1024
}

variable "desired_count" {
  description = "고정 태스크 수. 오토스케일링은 이번 단계 범위 아님"
  type        = number
  default     = 2
}

variable "ssm_parameter_arns" {
  description = "컨테이너에 시크릿으로 주입할 SSM 파라미터 {ENV_NAME = arn}"
  type        = map(string)
}

locals {
  container_name = "app"
}

data "aws_caller_identity" "current" {}

# SecureString 파라미터 복호화에 쓰이는 AWS 관리형 KMS 키
data "aws_kms_alias" "ssm" {
  name = "alias/aws/ssm"
}

# 컨테이너 stdout 로그 수집처
resource "aws_cloudwatch_log_group" "this" {
  name              = "/ecs/${var.name_prefix}"
  retention_in_days = 30
}

resource "aws_ecs_cluster" "this" {
  name = var.name_prefix

  # 태스크/서비스 단위 CPU·메모리 지표 수집 (오토스케일링 근거·부하 분석용)
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# ---------------------------------------------------------------------------
# IAM 역할 2종
#   - execution role: ECS 에이전트가 이미지 pull / 로그 write / 시크릿 read
#   - task role:      애플리케이션 코드가 호출하는 AWS API 용 (지금은 없음 -> 빈 역할)
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "ecs_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "execution" {
  name               = "${var.name_prefix}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

# ECR pull + CloudWatch Logs 기본 권한
resource "aws_iam_role_policy_attachment" "execution_managed" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# 이 앱의 파라미터만 읽도록 좁힌 인라인 정책
data "aws_iam_policy_document" "execution_secrets" {
  statement {
    sid       = "ReadAppParameters"
    actions   = ["ssm:GetParameters", "ssm:GetParameter"]
    resources = values(var.ssm_parameter_arns)
  }
  statement {
    sid       = "DecryptSecureString"
    actions   = ["kms:Decrypt"]
    resources = [data.aws_kms_alias.ssm.target_key_arn]
  }
}

resource "aws_iam_role_policy" "execution_secrets" {
  name   = "read-app-parameters"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.execution_secrets.json
}

# 애플리케이션 런타임 역할: 정책 의도적으로 비움.
# 앱이 S3/SQS 등을 호출하게 되면 그때 필요한 액션만 좁게 추가한다.
resource "aws_iam_role" "task" {
  name               = "${var.name_prefix}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

# ---------------------------------------------------------------------------
# Task Definition / Service
# ---------------------------------------------------------------------------
resource "aws_ecs_task_definition" "this" {
  family                   = var.name_prefix
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = local.container_name
      image     = var.image
      essential = true

      portMappings = [
        { containerPort = var.container_port, protocol = "tcp" }
      ]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" }
      ]

      # 값은 SSM에서 런타임에 주입 -> 태스크 정의에 평문이 남지 않음
      secrets = [
        for env_name, arn in var.ssm_parameter_arns : {
          name      = env_name
          valueFrom = arn
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.this.name
          "awslogs-region"        = var.region
          "awslogs-stream-prefix" = "app"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "this" {
  name            = "${var.name_prefix}-svc"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.this.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.subnet_ids
    security_groups  = [var.security_group_id]
    assign_public_ip = false # private 서브넷 -> ECR/CloudWatch/SSM 접근은 NAT 경유
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = local.container_name
    container_port   = var.container_port
  }

  # 앱 기동(+DB 스키마 생성) 시간 고려
  health_check_grace_period_seconds = 120

  # 최초 태스크 정의는 Terraform이 생성하지만,
  # 이후 이미지 교체(새 revision 등록 + 서비스 갱신)는 CI/CD가 담당한다.
  lifecycle {
    ignore_changes = [task_definition]
  }
}

output "cluster_name" {
  value = aws_ecs_cluster.this.name
}

output "cluster_arn" {
  value = aws_ecs_cluster.this.arn
}

output "service_name" {
  value = aws_ecs_service.this.name
}

output "task_family" {
  value = aws_ecs_task_definition.this.family
}

output "task_execution_role_arn" {
  value = aws_iam_role.execution.arn
}

output "task_role_arn" {
  value = aws_iam_role.task.arn
}

output "log_group_name" {
  value = aws_cloudwatch_log_group.this.name
}

output "container_name" {
  value = local.container_name
}
