variable "name_prefix" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  description = "ALB를 놓을 public 서브넷 (2개 이상, 서로 다른 AZ)"
  type        = list(string)
}

variable "security_group_id" {
  description = "alb SG (인터넷 80 허용)"
  type        = string
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "health_check_path" {
  type    = string
  default = "/actuator/health"
}

# 외부 진입점. 트래픽을 건강한 태스크로만 분배
resource "aws_lb" "this" {
  name               = "${var.name_prefix}-alb"
  internal           = false
  load_balancer_type = "application"
  subnets            = var.subnet_ids
  security_groups    = [var.security_group_id]
}

# ALB가 트래픽을 보낼 대상 그룹. Fargate(awsvpc)라 target_type = ip
resource "aws_lb_target_group" "this" {
  name        = "${var.name_prefix}-tg"
  port        = var.container_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  # /actuator/health 가 200이어야 트래픽을 받음
  health_check {
    path                = var.health_check_path
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  # 배포 시 기존 커넥션 정리 대기 시간 단축
  deregistration_delay = 30
}

# 80 포트로 들어온 요청을 대상 그룹으로 전달 (HTTPS/ACM은 도메인 확보 후 후속 작업)
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }
}

output "dns_name" {
  value = aws_lb.this.dns_name
}

output "alb_arn" {
  value = aws_lb.this.arn
}

output "target_group_arn" {
  value = aws_lb_target_group.this.arn
}

output "listener_arn" {
  value = aws_lb_listener.http.arn
}
