variable "repository_name" {
  type = string
}

# 컨테이너 이미지 저장소
resource "aws_ecr_repository" "this" {
  name = var.repository_name

  # dev: bootstrap 태그를 재사용할 수 있게 MUTABLE
  image_tag_mutability = "MUTABLE"

  # destroy 시 이미지가 남아있어도 삭제 허용
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

# 오래된 이미지 자동 정리 (스토리지 비용)
resource "aws_ecr_lifecycle_policy" "this" {
  repository = aws_ecr_repository.this.name
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "keep last 10 images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = { type = "expire" }
      }
    ]
  })
}

output "repository_url" {
  value = aws_ecr_repository.this.repository_url
}

output "repository_arn" {
  value = aws_ecr_repository.this.arn
}

output "repository_name" {
  value = aws_ecr_repository.this.name
}
