// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.controller.maintenance;


import com.yahoo.config.provision.zone.ZoneId;
import com.yahoo.vespa.hosted.controller.api.integration.noderepository.NodeMembership;
import com.yahoo.vespa.hosted.controller.api.integration.noderepository.NodeOwner;
import com.yahoo.vespa.hosted.controller.api.integration.noderepository.NodeRepositoryNode;
import com.yahoo.vespa.hosted.controller.api.integration.noderepository.NodeState;
import com.yahoo.vespa.hosted.controller.api.integration.noderepository.NodeType;
import com.yahoo.vespa.hosted.controller.integration.NodeRepositoryMock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChangeManagementAssessorTest {

    private ChangeManagementAssessor changeManagementAssessor = new ChangeManagementAssessor(new NodeRepositoryMock());

    @Test
    public void empty_input_variations() {
        ZoneId zone = ZoneId.from("prod", "eu-trd");
        List<String> hostNames = new ArrayList<>();
        List<NodeRepositoryNode> allNodesInZone = new ArrayList<>();

        // Both zone and hostnames are empty
        ChangeManagementAssessor.Assessment assessment
                = changeManagementAssessor.assessmentInner(hostNames, allNodesInZone, zone);
        assertEquals(0, assessment.getClusterAssessments().size());
    }

    @Test
    public void one_host_one_cluster_no_groups() {
        ZoneId zone = ZoneId.from("prod", "eu-trd");
        List<String> hostNames = Collections.singletonList("host1");
        List<NodeRepositoryNode> allNodesInZone = new ArrayList<>();
        allNodesInZone.add(createNode("node1", "host1", "default", 0 ));
        allNodesInZone.add(createNode("node2", "host1", "default", 0 ));
        allNodesInZone.add(createNode("node3", "host1", "default", 0 ));

        // Add an not impacted hosts
        allNodesInZone.add(createNode("node4", "host2", "default", 0 ));

        // Add tenant hosts
        allNodesInZone.add(createHost("host1", NodeType.host));
        allNodesInZone.add(createHost("host2", NodeType.host));

        // Make Assessment
        List<ChangeManagementAssessor.ClusterAssessment> assessments
                = changeManagementAssessor.assessmentInner(hostNames, allNodesInZone, zone).getClusterAssessments();

        // Assess the assessment :-o
        assertEquals(1, assessments.size());
        assertEquals(3, assessments.get(0).clusterImpact);
        assertEquals(4, assessments.get(0).clusterSize);
        assertEquals(1, assessments.get(0).groupsImpact);
        assertEquals(1, assessments.get(0).groupsTotal);
        assertEquals("content:default", assessments.get(0).cluster);
        assertEquals("mytenant:myapp:default", assessments.get(0).app);
        assertEquals("prod.eu-trd", assessments.get(0).zone);
    }

    @Test
    public void one_of_two_groups_in_one_of_two_clusters() {
        ZoneId zone = ZoneId.from("prod", "eu-trd");
        List<String> hostNames = Arrays.asList("host1", "host2");
        List<NodeRepositoryNode> allNodesInZone = new ArrayList<>();

        // Two impacted nodes on host1
        allNodesInZone.add(createNode("node1", "host1", "default", 0 ));
        allNodesInZone.add(createNode("node2", "host1", "default", 0 ));

        // One impacted nodes on host2
        allNodesInZone.add(createNode("node3", "host2", "default", 0 ));

        // Another group on hosts not impacted
        allNodesInZone.add(createNode("node4", "host3", "default", 1 ));
        allNodesInZone.add(createNode("node5", "host3", "default", 1 ));
        allNodesInZone.add(createNode("node6", "host3", "default", 1 ));

        // Another cluster on hosts not impacted - this one also with three different groups (should all be ignored here)
        allNodesInZone.add(createNode("node4", "host4", "myman", 4 ));
        allNodesInZone.add(createNode("node5", "host4", "myman", 5 ));
        allNodesInZone.add(createNode("node6", "host4", "myman", 6 ));

        // Add tenant hosts
        allNodesInZone.add(createHost("host1", NodeType.host));
        allNodesInZone.add(createHost("host2", NodeType.host));


        // Make Assessment
        ChangeManagementAssessor.Assessment assessment
                = changeManagementAssessor.assessmentInner(hostNames, allNodesInZone, zone);

        // Assess the assessment :-o
        List<ChangeManagementAssessor.ClusterAssessment> clusterAssessments = assessment.getClusterAssessments();
        assertEquals(1, clusterAssessments.size()); //One cluster is impacted
        assertEquals(3, clusterAssessments.get(0).clusterImpact);
        assertEquals(6, clusterAssessments.get(0).clusterSize);
        assertEquals(1, clusterAssessments.get(0).groupsImpact);
        assertEquals(2, clusterAssessments.get(0).groupsTotal);
        assertEquals("content:default", clusterAssessments.get(0).cluster);
        assertEquals("mytenant:myapp:default", clusterAssessments.get(0).app);
        assertEquals("prod.eu-trd", clusterAssessments.get(0).zone);
        assertEquals("Impact not larger than upgrade policy", clusterAssessments.get(0).impact);

        List<ChangeManagementAssessor.HostAssessment> hostAssessments = assessment.getHostAssessments();
        assertEquals(2, hostAssessments.size());
        assertTrue(hostAssessments.stream().anyMatch(hostAssessment ->
                hostAssessment.hostName.equals("host1") &&
                hostAssessment.switchName.equals("switch1") &&
                hostAssessment.numberOfChildren == 2 &&
                hostAssessment.numberOfProblematicChildren == 2
        ));
    }

    @Test
    public void config_and_proxy_hosts() {
        var zone = ZoneId.from("prod", "eu-trd");
        var hostNames = Arrays.asList("config1", "config2", "routing1");
        var allNodesInZone = new ArrayList<NodeRepositoryNode>();


        // Add config nodes and parents
        allNodesInZone.add(createNode("config1", "confighost1", "config", 0, NodeType.config));
        allNodesInZone.add(createHost("confighost1", NodeType.confighost));
        allNodesInZone.add(createNode("config2", "confighost2", "config", 0, NodeType.config));
        allNodesInZone.add(createHost("confighost2", NodeType.confighost));

        // Add routing nodes and parents
        allNodesInZone.add(createNode("routing1", "parentrouting1", "routing", 0, NodeType.proxy));
        allNodesInZone.add(createHost("parentrouting1", NodeType.proxyhost));
        allNodesInZone.add(createNode("routing2", "parentrouting2", "routing", 0, NodeType.proxy));
        allNodesInZone.add(createHost("parentrouting2", NodeType.proxyhost));
        allNodesInZone.add(createNode("routing3", "parentrouting3", "routing", 0, NodeType.proxy));
        allNodesInZone.add(createHost("parentrouting3", NodeType.proxyhost));

        var assessment = changeManagementAssessor.assessmentInner(hostNames, allNodesInZone, zone);

        var clusterAssessments = assessment.getClusterAssessments();
        assertEquals(2, clusterAssessments.size());

        var configAssessment = clusterAssessments.get(0);
        assertEquals("Large impact. Consider reprovisioning one or more config servers", configAssessment.impact);
        assertEquals(2, configAssessment.clusterImpact);

        var routingAssessment = clusterAssessments.get(1);
        assertEquals("33% of routing nodes impacted. Consider reprovisioning if too many", routingAssessment.impact);
    }

    private NodeOwner createOwner() {
        NodeOwner owner = new NodeOwner();
        owner.tenant = "mytenant";
        owner.application = "myapp";
        owner.instance = "default";
        return owner;
    }

    private NodeMembership createMembership(String clusterId, int group) {
        NodeMembership membership = new NodeMembership();
        membership.group = "" + group;
        membership.clusterid = clusterId;
        membership.clustertype = "content";
        membership.index = 2;
        membership.retired = false;
        return membership;
    }

    private NodeRepositoryNode createNode(String nodename, String hostname, String clusterId, int group) {
        return createNode(nodename, hostname, clusterId, group, NodeType.tenant);
    }

    private NodeRepositoryNode createNode(String nodename, String hostname, String clusterId, int group, NodeType nodeType) {
        NodeRepositoryNode node = new NodeRepositoryNode();
        node.setHostname(nodename);
        node.setParentHostname(hostname);
        node.setState(NodeState.active);
        node.setOwner(createOwner());
        node.setMembership(createMembership(clusterId, group));
        node.setType(nodeType);

        return node;
    }

    private NodeRepositoryNode createHost(String hostname, NodeType nodeType) {
        NodeRepositoryNode node = new NodeRepositoryNode();
        node.setHostname(hostname);
        node.setSwitchHostname("switch1");
        node.setType(nodeType);
        node.setOwner(createOwner());
        node.setMembership(createMembership(nodeType.name(), 0));
        return node;
    }
}
