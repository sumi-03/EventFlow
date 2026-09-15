locals {
  name_prefix = "${var.project}-${var.env}"
}

# ---------------------------------------------------------------------------
# 1) 네트워크: VPC / 서브넷 / 라우팅 / 보안그룹 3계층
# ---------------------------------------------------------------------------
module "network" {
  source      = "../../modules/network"
  name_prefix = local.name_prefix
  vpc_cidr    = var.vpc_cidr
  azs         = var.azs
  app_port    = var.container_port
}

# ---------------------------------------------------------------------------
# 2) 데이터베이스: RDS PostgreSQL (private 서브넷)
# ---------------------------------------------------------------------------
resource "random_password" "db" {
  length  = 24
  special = false
}

module "database" {
  source            = "../../modules/database"
  name_prefix       = local.name_prefix
  subnet_ids        = module.network.private_subnet_ids
  security_group_id = module.network.rds_security_group_id
  instance_class    = var.db_instance_class
  allocated_storage = var.db_allocated_storage
  multi_az          = var.db_multi_az
  db_name           = "eventflow"
  db_username       = "eventflow"
  db_password       = random_password.db.result
}

# ---------------------------------------------------------------------------
# 2-1) 대기열: ElastiCache Redis (private 서브넷, 단일 노드)
# ---------------------------------------------------------------------------
module "elasticache" {
  source            = "../../modules/elasticache"
  name_prefix       = local.name_prefix
  subnet_ids        = module.network.private_subnet_ids
  security_group_id = module.network.redis_security_group_id
}

# ---------------------------------------------------------------------------
# 3) 앱 설정값: SSM Parameter Store (ECS가 컨테이너에 시크릿으로 주입)
# ---------------------------------------------------------------------------
resource "aws_ssm_parameter" "db_url" {
  name  = "/${var.project}/${var.env}/db/url"
  type  = "String"
  value = "jdbc:postgresql://${module.database.address}:${module.database.port}/${module.database.db_name}"
}

resource "aws_ssm_parameter" "db_username" {
  name  = "/${var.project}/${var.env}/db/username"
  type  = "String"
  value = module.database.db_username
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/${var.project}/${var.env}/db/password"
  type  = "SecureString"
  value = random_password.db.result
}

resource "aws_ssm_parameter" "jwt_secret" {
  name  = "/${var.project}/${var.env}/jwt/secret"
  type  = "SecureString"
  value = var.jwt_secret
}

resource "aws_ssm_parameter" "redis_host" {
  name  = "/${var.project}/${var.env}/redis/host"
  type  = "String"
  value = module.elasticache.endpoint
}

resource "aws_ssm_parameter" "redis_port" {
  name  = "/${var.project}/${var.env}/redis/port"
  type  = "String"
  value = tostring(module.elasticache.port)
}

# ---------------------------------------------------------------------------
# 4) 이미지 저장소: ECR
# ---------------------------------------------------------------------------
module "ecr" {
  source          = "../../modules/ecr"
  repository_name = var.project
}

# ---------------------------------------------------------------------------
# 5) 로드밸런서: ALB + Target Group + Listener(80)
# ---------------------------------------------------------------------------
module "alb" {
  source            = "../../modules/alb"
  name_prefix       = local.name_prefix
  vpc_id            = module.network.vpc_id
  subnet_ids        = module.network.public_subnet_ids
  security_group_id = module.network.alb_security_group_id
  container_port    = var.container_port
  health_check_path = "/actuator/health"
}

# ---------------------------------------------------------------------------
# 6) 실행: ECS Fargate 클러스터 / 태스크 정의 / 서비스(태스크 2개 고정)
#    태스크는 private 서브넷에 배치 - 인터넷에서 직접 도달 불가, 아웃바운드는 NAT 경유
# ---------------------------------------------------------------------------
module "ecs" {
  source            = "../../modules/ecs"
  name_prefix       = local.name_prefix
  region            = var.aws_region
  subnet_ids        = module.network.private_subnet_ids
  security_group_id = module.network.app_security_group_id
  target_group_arn  = module.alb.target_group_arn
  image             = "${module.ecr.repository_url}:${var.container_image_tag}"
  container_port    = var.container_port
  cpu               = var.container_cpu
  memory            = var.container_memory
  desired_count     = var.desired_count

  enable_autoscaling       = var.enable_autoscaling
  autoscaling_min_capacity = var.autoscaling_min_capacity
  autoscaling_max_capacity = var.autoscaling_max_capacity
  autoscaling_cpu_target   = var.autoscaling_cpu_target

  ssm_parameter_arns = {
    DB_URL      = aws_ssm_parameter.db_url.arn
    DB_USERNAME = aws_ssm_parameter.db_username.arn
    DB_PASSWORD = aws_ssm_parameter.db_password.arn
    JWT_SECRET  = aws_ssm_parameter.jwt_secret.arn
    REDIS_HOST  = aws_ssm_parameter.redis_host.arn
    REDIS_PORT  = aws_ssm_parameter.redis_port.arn
  }

  # 리스너가 만들어진 뒤에 서비스가 대상 등록을 시도하도록
  depends_on = [module.alb]
}

# ---------------------------------------------------------------------------
# 7) CI/CD: GitHub Actions OIDC 신뢰 + 최소권한 배포 역할
# ---------------------------------------------------------------------------
module "cicd" {
  source                  = "../../modules/cicd"
  name_prefix             = local.name_prefix
  region                  = var.aws_region
  github_repo             = var.github_repo
  github_branch           = var.github_branch
  ecr_repository_arn      = module.ecr.repository_arn
  ecs_cluster_name        = module.ecs.cluster_name
  ecs_service_name        = module.ecs.service_name
  task_execution_role_arn = module.ecs.task_execution_role_arn
  task_role_arn           = module.ecs.task_role_arn
}
