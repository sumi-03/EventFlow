provider "aws" {
  region = var.aws_region

  # 모든 리소스에 공통 태그 (비용 분류/정리에 사용)
  default_tags {
    tags = {
      Project   = var.project
      Env       = var.env
      ManagedBy = "terraform"
    }
  }
}
