package com.ipaixao.stack;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.rds.InstanceProps;
import software.constructs.Construct;

import static java.util.Collections.singletonList;

public class RdsStack extends Stack {

    public RdsStack(final Construct scope, final String id, Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public RdsStack(final Construct scope, final String id, final StackProps props, Vpc vpc) {
        super(scope, id, props);

        final var databasePassword = CfnParameter.Builder.create(this, "databasePassword")
                .type("String")
                .description("The RDS instance password")
                .build();

        final var securityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));

        DatabaseCluster.Builder
                .create(this, "ibeer-db")
                .clusterIdentifier("ibeer-db")
                .instances(3)
                .engine(DatabaseClusterEngine.auroraMysql(
                                AuroraMysqlClusterEngineProps.builder()
                                        .version(AuroraMysqlEngineVersion.VER_2_08_1)
                                        .build()
                        )
                )
                .defaultDatabaseName("ibeer")
                .credentials(
                        Credentials.fromUsername("admin",
                                CredentialsFromUsernameOptions.builder()
                                        .password(SecretValue.plainText(databasePassword.getValueAsString()))
                                        .build()
                        )
                )
                .instanceProps(
                        InstanceProps.builder()
                                .vpc(vpc)
                                .publiclyAccessible(true)
                                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.SMALL))
                                .securityGroups(singletonList(securityGroup))
                                .vpcSubnets(
                                        SubnetSelection.builder()
                                                .subnets(vpc.getPublicSubnets())
                                                .build()
                                )
                                .build()
                )
                .build();

//        CfnOutput.Builder.create(this, "rds-endpoint")
//                .exportName("rds-endpoint")
//                .value(databaseCluster.)
//                .build();
        CfnOutput.Builder.create(this, "rds-password")
                .exportName("rds-password")
                .value(databasePassword.getValueAsString())
                .build();


    }
}
