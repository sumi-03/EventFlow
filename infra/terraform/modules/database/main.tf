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

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids  = [var.security_group_id]
  publicly_accessible    = false
  multi_az               = false # dev: 비용 절약 (프로덕션이면 true)

  backup_retention_period = 1
  skip_final_snapshot     = true  # dev: destroy 시 스냅샷 생략
  deletion_protection     = false # dev: destroy 허용
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
