terraform {
  # S3 네이티브 state locking(use_lockfile)은 Terraform 1.10+ 필요
  required_version = ">= 1.10"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project   = "eventflow"
      ManagedBy = "terraform"
      Component = "tf-backend"
    }
  }
}

variable "aws_region" {
  description = "리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "state_bucket_name" {
  description = "원격 state 버킷명"
  type        = string
}

# state 파일 저장소
# 잠금(lock)은 별도 리소스가 필요 없다: S3 backend의 use_lockfile 옵션이
# 이 버킷에 조건부 쓰기로 .tflock 객체를 만들어 동시 apply를 막는다.
resource "aws_s3_bucket" "state" {
  bucket = var.state_bucket_name
}

# 실수로 덮어써도 이전 버전 복구 가능
resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id
  versioning_configuration {
    status = "Enabled"
  }
}

# state 안에 DB 비밀번호 등 평문이 들어가므로 저장 시 암호화
resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# 버킷 전체 공개 차단
resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

output "state_bucket" {
  value = aws_s3_bucket.state.id
}
