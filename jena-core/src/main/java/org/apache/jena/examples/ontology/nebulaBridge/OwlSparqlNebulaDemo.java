package org.apache.jena.examples.ontology.nebulaBridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

/**
 * Demo pipeline:
 * 1) OWL/RDF ontology model in Jena.
 * 2) Agent skill outputs SPARQL.
 * 3) Translate SPARQL pattern to NebulaGraph nGQL.
 * 4) Execute nGQL (mock client shown, can replace with real nebula-java client).
 */
public class OwlSparqlNebulaDemo {

    private static final String PREFIX = "PREFIX ex: <http://example.com/ict#>\n";

    public static void main(String[] args) {
        String ttl = args.length > 0 ? args[0] : "jena-core/src-examples/data/ontology_graph_demo.ttl";
        Model model = RDFDataMgr.loadModel(ttl);
        Dataset dataset = DatasetFactory.create(model);

        String skillIntent = "查询告警以及其归属的异常状态特征";
        String sparql = AgentSkill.toSparql(skillIntent);
        System.out.println("=== Skill -> SPARQL ===\n" + sparql);

        List<String> rows = runSparql(dataset, sparql);
        rows.forEach(System.out::println);

        String ngql = SparqlToNgqlTranslator.toNgql(sparql);
        System.out.println("\n=== SPARQL -> nGQL ===\n" + ngql);

        NebulaClient nebulaClient = new NebulaClient();
        NebulaResult result = nebulaClient.execute(ngql);
        System.out.println("\n=== nGQL execution (demo) ===");
        result.rows().forEach(System.out::println);

        writeBootstrapNgql("jena-core/src-examples/data/ontology_graph_bootstrap.ngql");
    }

    static List<String> runSparql(Dataset dataset, String sparql) {
        Query query = QueryFactory.create(sparql);
        List<String> lines = new ArrayList<>();
        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                lines.add(String.format(Locale.ROOT, "alarm=%s, pattern=%s", qs.get("alarm"), qs.get("pattern")));
            }
        }
        return lines;
    }

    private static void writeBootstrapNgql(String outPath) {
        String ddl = """
                -- 1) Schema for ontology instances in NebulaGraph
                CREATE SPACE IF NOT EXISTS ict_kg(partition_num=10, replica_factor=1, vid_type=FIXED_STRING(64));
                USE ict_kg;
                CREATE TAG IF NOT EXISTS Site(name string);
                CREATE TAG IF NOT EXISTS Ne(name string);
                CREATE TAG IF NOT EXISTS Alarm(alarmName string);
                CREATE TAG IF NOT EXISTS AbnormalStatusPattern(name string);

                CREATE EDGE IF NOT EXISTS LOCATED_IN();
                CREATE EDGE IF NOT EXISTS HAPPEN();
                CREATE EDGE IF NOT EXISTS HAPPEN_ON();
                CREATE EDGE IF NOT EXISTS CLASSIFY();
                CREATE EDGE IF NOT EXISTS TRIGGER();
                CREATE EDGE IF NOT EXISTS NE_LINK();

                -- 2) Example physical data mapping (RDF instances -> graph vertices/edges)
                INSERT VERTEX Site(name) VALUES "Site_SZ":("Shenzhen Site");
                INSERT VERTEX Ne(name) VALUES "NE_001":("NE_001"), "NE_002":("NE_002");
                INSERT VERTEX Alarm(alarmName) VALUES "Alarm_LinkDown_001":("Ethernet Physical Port down"),
                                                     "Alarm_Bandwidth_002":("Send bandwidth usage threshold crossed");
                INSERT VERTEX AbnormalStatusPattern(name) VALUES "Pattern_TransmissionFailure":("Transmission failure"),
                                                                  "Pattern_Congestion":("Congestion");

                INSERT EDGE LOCATED_IN() VALUES "NE_001"->"Site_SZ":(), "NE_002"->"Site_SZ":();
                INSERT EDGE NE_LINK() VALUES "NE_002"->"NE_001":();
                INSERT EDGE HAPPEN_ON() VALUES "Alarm_LinkDown_001"->"NE_001":(), "Alarm_Bandwidth_002"->"NE_002":();
                INSERT EDGE CLASSIFY() VALUES "Alarm_LinkDown_001"->"Pattern_TransmissionFailure":(),
                                              "Alarm_Bandwidth_002"->"Pattern_Congestion":();
                INSERT EDGE TRIGGER() VALUES "Alarm_LinkDown_001"->"Alarm_Bandwidth_002":();
                """;
        try {
            Files.writeString(Path.of(outPath), ddl);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write bootstrap nGQL file", e);
        }
    }

    static class AgentSkill {
        static String toSparql(String intent) {
            if (intent.contains("异常状态特征")) {
                return PREFIX + """
                        SELECT ?alarm ?pattern WHERE {
                          ?alarm a ex:Alarm .
                          ?alarm ex:classify ?pattern .
                        }
                        """;
            }
            return PREFIX + "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10";
        }
    }

    static class SparqlToNgqlTranslator {
        static String toNgql(String sparql) {
            String compact = sparql.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (compact.contains("?alarm ex:classify ?pattern".toLowerCase(Locale.ROOT))) {
                return "MATCH (a:Alarm)-[:CLASSIFY]->(p:AbnormalStatusPattern) RETURN id(a) AS alarm, id(p) AS pattern;";
            }
            return "MATCH (v) RETURN id(v) LIMIT 10;";
        }
    }

    record NebulaResult(List<String> rows) {}

    static class NebulaClient {
        NebulaResult execute(String ngql) {
            // Replace with official nebula-java client integration in production.
            return new NebulaResult(List.of(
                    "[mock] connected to NebulaGraph",
                    "[mock] execute => " + ngql,
                    "[mock] row: alarm=Alarm_LinkDown_001, pattern=Pattern_TransmissionFailure",
                    "[mock] row: alarm=Alarm_Bandwidth_002, pattern=Pattern_Congestion"));
        }
    }
}
