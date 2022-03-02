package com.ipaixao;

import com.ipaixao.service.ServiceStack;
import com.ipaixao.stack.ClusterStack;
import com.ipaixao.stack.VpcStack;
import software.amazon.awscdk.core.App;

public class IbeerPipelineApp {
    public static void main(final String[] args) {
        final var app = new App();

        final var vpcStack = new VpcStack(app, "ibeer-vpc");

        final var clusterStack = new ClusterStack(app, "ibeer-cluster", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack);

        final var serviceStack = new ServiceStack(app, "ibeer-service", clusterStack.getCluster());
        serviceStack.addDependency(clusterStack);

        app.synth();
    }
}

