output "alb_url" {
  description = "서비스 진입 URL"
  value       = "http://${module.alb.dns_name}"
}

output "ecr_repository_url" {
  value = module.ecr.repository_url
}

output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}

output "ecs_service_name" {
  value = module.ecs.service_name
}

output "ecs_task_family" {
  value = module.ecs.task_family
}

output "ecs_container_name" {
  value = module.ecs.container_name
}

output "gha_deploy_role_arn" {
  description = "GitHub Actions 변수 AWS_DEPLOY_ROLE_ARN 에 넣을 값"
  value       = module.cicd.deploy_role_arn
}

output "rds_endpoint" {
  value     = module.database.endpoint
  sensitive = true
}

# private 서브넷(ECS 태스크)에서 나가는 트래픽의 공인 출발지 IP (AZ별)
output "nat_public_ips" {
  value = module.network.nat_public_ips
}

# GitHub Actions repository variables 로 그대로 복사할 값 모음
output "github_actions_variables" {
  value = {
    AWS_REGION          = var.aws_region
    AWS_DEPLOY_ROLE_ARN = module.cicd.deploy_role_arn
    ECR_REPOSITORY      = module.ecr.repository_name
    ECS_CLUSTER         = module.ecs.cluster_name
    ECS_SERVICE         = module.ecs.service_name
    ECS_TASK_FAMILY     = module.ecs.task_family
    ECS_CONTAINER       = module.ecs.container_name
  }
}
