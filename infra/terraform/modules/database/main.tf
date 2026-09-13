variable "name_prefix" {
  type = string
}

variable "subnet_ids" {
  description = "RDS를 놓을 private 서브넷 목록"
  type        = list(string)
}

variable "security_group_id" {
  description = "rds SG (app 태스크에서만 5432 허용)"
  type        = string
}

variable "instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "allocated_storage" {
  type    = number
  default = 20
}

variable "engine_version" {
  type    = string
  default = "16"
}

variable "multi_az" {
  description = "다른 AZ에 동기 standby + 자동 장애조치(60~120s). prod 기본값 true"
  type        = bool
  default     = true
}

variable "backup_retention_period" {
  description = "자동 백업 보관 일수"
  type        = number
  default     = 7
}

variable "deletion_protection" {
  description = "삭제 방지. dev에서 terraform destroy 하려면 false 유지 필요, prod면 true"
  type        = bool
  default     = false
}

variable "skip_final_snapshot" {
  description = "삭제 시 최종 스냅샷 생략. prod면 false"
  type        = bool
  default     = true
}

variable "db_name" {
  type    = string
  default = "eventflow"
}

variable "db_username" {
  type    = string
  default = "eventflow"
}

variable "db_password" {
  type      = string
  sensitive = true
}

# private 서브넷 묶음 - RDS는 이 안에서만 생성됨
resource "aws_db_subnet_group" "this" {
  name       = "${var.name_prefix}-db"
  subnet_ids = var.subnet_ids
}

resource "aws_db_instance" "this" {
  identifier     = "${var.name_prefix}-postgres"
  engine         = "postgres"
  engine_version = var.engine_version
  instance_class = var.instance_class

  allocated_storage = var.allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true # 저장 데이터 암호화 (기본값 false — 명시). 미지정 시 aws/rds KMS 키 사용

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [var.security_group_id]
  publicly_accessible    = false
  multi_az               = var.multi_az

  backup_retention_period = var.backup_retention_period
  skip_final_snapshot     = var.skip_final_snapshot
  deletion_protection     = var.deletion_protection
  apply_immediately       = true

  tags = { Name = "${var.name_prefix}-postgres" }
}

output "address" {
  value = aws_db_instance.this.address
}

output "port" {
  value = aws_db_instance.this.port
}

output "db_name" {
  value = aws_db_instance.this.db_name
}

output "db_username" {
  value = var.db_username
}

output "endpoint" {
  value = aws_db_instance.this.endpoint
}
