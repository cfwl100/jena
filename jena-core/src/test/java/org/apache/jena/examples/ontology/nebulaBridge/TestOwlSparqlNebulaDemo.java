package org.apache.jena.examples.ontology.nebulaBridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

public class TestOwlSparqlNebulaDemo {

    @Test
    public void testSkillToSparql() {
        String sparql = OwlSparqlNebulaDemo.AgentSkill.toSparql("查询告警以及其归属的异常状态特征");
        assertTrue(sparql.contains("SELECT ?alarm ?pattern"));
        assertTrue(sparql.contains("ex:classify"));
    }

    @Test
    public void testSparqlToNgql() {
        String sparql = OwlSparqlNebulaDemo.AgentSkill.toSparql("查询告警以及其归属的异常状态特征");
        String ngql = OwlSparqlNebulaDemo.SparqlToNgqlTranslator.toNgql(sparql);
        assertEquals("MATCH (a:Alarm)-[:CLASSIFY]->(p:AbnormalStatusPattern) RETURN id(a) AS alarm, id(p) AS pattern;", ngql);
    }

    @Test
    public void testRunSparqlOverOntologyData() {
        Model model = RDFDataMgr.loadModel("jena-core/src-examples/data/ontology_graph_demo.ttl");
        Dataset dataset = DatasetFactory.create(model);
        String sparql = OwlSparqlNebulaDemo.AgentSkill.toSparql("查询告警以及其归属的异常状态特征");

        List<String> rows = OwlSparqlNebulaDemo.runSparql(dataset, sparql);
        assertEquals(2, rows.size());
        assertTrue(rows.stream().anyMatch(x -> x.contains("Alarm_LinkDown_001") && x.contains("Pattern_TransmissionFailure")));
        assertTrue(rows.stream().anyMatch(x -> x.contains("Alarm_Bandwidth_002") && x.contains("Pattern_Congestion")));
    }

    @Test
    public void testMockNebulaClient() {
        OwlSparqlNebulaDemo.NebulaResult rs = new OwlSparqlNebulaDemo.NebulaClient().execute("MATCH (v) RETURN id(v) LIMIT 10;");
        assertEquals(4, rs.rows().size());
        assertTrue(rs.rows().get(0).contains("connected"));
    }
}
