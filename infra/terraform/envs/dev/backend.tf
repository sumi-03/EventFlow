terraform {
  # S3 네이티브 state locking(use_lockfile)은 Terraform 1.10+ 필요
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # state는 로컬이 아니라 S3에 저장하고, S3 네이티브 lockfile로 잠근다.
  # bootstrap 적용 후 아래 bucket 값을 실제 이름으로 바꾸고 `terraform init`.
  backend "s3" {
    bucket       = "eventflow-tfstate-0119"
    key          = "envs/dev/terraform.tfstate"
    region       = "ap-northeast-2"
    use_lockfile = true # S3 조건부 쓰기로 잠금 (TF 1.10+)
    encrypt      = true
  }
}
