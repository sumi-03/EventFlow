variable "name_prefix" {
  type = string
}

variable "subnet_ids" {
  description = "Redis를 놓을 private 서브넷 목록"
  type        = list(string)
}

variable "security_group_id" {
  description = "redis SG (app 태스크에서만 6379 허용)"
  type        = string
}

variable "node_type" {
  type    = string
  default = "cache.t4g.micro"
}

variable "engine_version" {
  type    = string
  default = "7.1"
}

# private 서브넷 묶음 - Redis는 이 안에서만 생성됨
resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.name_prefix}-redis"
  subnet_ids = var.subnet_ids
}

# 단일 노드. 대기열 상태가 유실돼도 다시 줄 서면 되는 수준이라 복제/Multi-AZ는 이번 단계 범위 아님
resource "aws_elasticache_cluster" "this" {
  cluster_id      = "${var.name_prefix}-redis"
  engine          = "redis"
  engine_version  = var.engine_version
  node_type       = var.node_type
  num_cache_nodes = 1
  port            = 6379

  subnet_group_name  = aws_elasticache_subnet_group.this.name
  security_group_ids = [var.security_group_id]

  tags = { Name = "${var.name_prefix}-redis" }
}

output "endpoint" {
  value = aws_elasticache_cluster.this.cache_nodes[0].address
}

output "port" {
  value = aws_elasticache_cluster.this.cache_nodes[0].port
}
