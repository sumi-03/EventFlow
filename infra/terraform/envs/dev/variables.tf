variable "project" {
  type    = string
  default = "eventflow"
}

variable "env" {
  type    = string
  default = "dev"
}

variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "azs" {
  type    = list(string)
  default = ["ap-northeast-2a", "ap-northeast-2c"]
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "db_allocated_storage" {
  type    = number
  default = 20
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "container_cpu" {
  type    = number
  default = 512
}

variable "container_memory" {
  type    = number
  default = 1024
}

variable "desired_count" {
  description = "고정 태스크 수 (오토스케일링 미사용)"
  type        = number
  default     = 2
}

variable "container_image_tag" {
  description = "배포할 이미지 태그. 최초에는 bootstrap, 이후 CI가 git sha로 갱신"
  type        = string
  default     = "bootstrap"
}

variable "jwt_secret" {
  description = "운영 JWT 서명 키 (HS256, 충분히 길게). tfvars 또는 TF_VAR_jwt_secret 로 주입"
  type        = string
  sensitive   = true
}

variable "github_repo" {
  type    = string
  default = "sumi-03/EventFlow"
}

variable "github_branch" {
  type    = string
  default = "main"
}
