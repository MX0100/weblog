output "load_balancer_url" {
  description = "URL of the load balancer"
  value       = "http://${aws_lb.main.dns_name}"
}

output "ecr_frontend_repository_url" {
  description = "URL of the ECR repository for frontend"
  value       = aws_ecr_repository.frontend.repository_url
}

output "ecr_backend_repository_url" {
  description = "URL of the ECR repository for backend"
  value       = aws_ecr_repository.backend.repository_url
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.main.name
} 