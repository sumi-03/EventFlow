variable "name_prefix" {
  description = "리소스 이름 접두사 (예: eventflow-dev)"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC 대역"
  type        = string
  default     = "10.0.0.0/16"
}

variable "azs" {
  description = "사용할 가용영역 목록 (2개 권장)"
  type        = list(string)
}

variable "app_port" {
  description = "컨테이너 포트"
  type        = number
  default     = 8080
}

# 격리된 사설 네트워크
resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags                 = { Name = "${var.name_prefix}-vpc" }
}

# 인터넷 출입구
resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = "${var.name_prefix}-igw" }
}

# public 서브넷: ALB + ECS 태스크 (NAT 없이 공인 IP로 아웃바운드)
resource "aws_subnet" "public" {
  count                   = length(var.azs)
  vpc_id                  = aws_vpc.this.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone       = var.azs[count.index]
  map_public_ip_on_launch = true
  tags = {
    Name = "${var.name_prefix}-public-${var.azs[count.index]}"
    Tier = "public"
  }
}

# private 서브넷: RDS 전용 (외부에서 도달 불가)
resource "aws_subnet" "private" {
  count             = length(var.azs)
  vpc_id            = aws_vpc.this.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 10)
  availability_zone = var.azs[count.index]
  tags = {
    Name = "${var.name_prefix}-private-${var.azs[count.index]}"
    Tier = "private"
  }
}

# public 라우팅: 0.0.0.0/0 -> IGW
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }
  tags = { Name = "${var.name_prefix}-public-rt" }
}

resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# private 라우팅: VPC 로컬만 (NAT 미사용, 인터넷 경로 없음)
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = "${var.name_prefix}-private-rt" }
}

resource "aws_route_table_association" "private" {
  count          = length(aws_subnet.private)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

# 1계층: 인터넷 -> ALB (80)
resource "aws_security_group" "alb" {
  name_prefix = "${var.name_prefix}-alb-"
  vpc_id      = aws_vpc.this.id
  description = "ALB - allow HTTP from internet"

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle { create_before_destroy = true }
  tags = { Name = "${var.name_prefix}-alb-sg" }
}

# 2계층: ALB -> ECS 태스크 (앱 포트). ALB SG에서 오는 것만 허용
resource "aws_security_group" "app" {
  name_prefix = "${var.name_prefix}-app-"
  vpc_id      = aws_vpc.this.id
  description = "ECS tasks - allow app port from ALB only"

  ingress {
    description     = "App port from ALB"
    from_port       = var.app_port
    to_port         = var.app_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle { create_before_destroy = true }
  tags = { Name = "${var.name_prefix}-app-sg" }
}

# 3계층: ECS 태스크 -> RDS (5432). app SG에서 오는 것만 허용
resource "aws_security_group" "rds" {
  name_prefix = "${var.name_prefix}-rds-"
  vpc_id      = aws_vpc.this.id
  description = "RDS - allow PostgreSQL from app tasks only"

  ingress {
    description     = "PostgreSQL from app"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle { create_before_destroy = true }
  tags = { Name = "${var.name_prefix}-rds-sg" }
}

output "vpc_id" {
  value = aws_vpc.this.id
}

output "public_subnet_ids" {
  value = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  value = aws_subnet.private[*].id
}

output "alb_security_group_id" {
  value = aws_security_group.alb.id
}

output "app_security_group_id" {
  value = aws_security_group.app.id
}

output "rds_security_group_id" {
  value = aws_security_group.rds.id
}
