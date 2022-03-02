package com.ipaixao.service;

import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

public class ServiceStack extends Stack {
    public ServiceStack(final Construct scope, final String id, Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public ServiceStack(final Construct scope, final String id, final StackProps props, Cluster cluster) {
        super(scope, id, props);

        final var service = ApplicationLoadBalancedFargateService.Builder.create(this, "ALB-ibeer")
                .serviceName("ibeer-service")
                .cluster(cluster)
                .cpu(512)
                .memoryLimitMiB(1024)
                .desiredCount(2)
                .listenerPort(8081)
                .assignPublicIp(true)
                .taskImageOptions(alb())
                .publicLoadBalancer(true)
                .build();

        service.getTargetGroup().configureHealthCheck(
                new HealthCheck.Builder()
                        .path("/actuator/health")
                        .port("8081")
                        .healthyHttpCodes("200")
                        .build()
        );

        final var autoScaleTaskCount = service.getService().autoScaleTaskCount(
                EnableScalingProps.builder()
                        .minCapacity(2)
                        .maxCapacity(4)
                        .build()
        );

        autoScaleTaskCount.scaleOnCpuUtilization("ibeer-service-auto-scaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build()
        );
    }

    private ApplicationLoadBalancedTaskImageOptions alb() {
        return ApplicationLoadBalancedTaskImageOptions.builder()
                .containerName("ibeer-app")
                .image(ContainerImage.fromRegistry("ipaixao/i-beer:0.0.2"))
                .containerPort(8081)
                .logDriver(logDriver())
                .build();
    }

    private LogDriver logDriver() {
        return LogDriver.awsLogs(
                AwsLogDriverProps.builder()
                        .logGroup(logGroup())
                        .streamPrefix("ibeer-service")
                        .build()
        );
    }

    private LogGroup logGroup() {
        return LogGroup.Builder.create(this, "LogGroup-ibeer")
                .logGroupName("ibeer-service")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }
}
