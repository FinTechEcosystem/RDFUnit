package org.aksw.rdfunit.utils;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.rdfunit.elements.implementations.PatternImpl;
import org.aksw.rdfunit.elements.implementations.PatternParameterImpl;
import org.aksw.rdfunit.elements.interfaces.Pattern;
import org.aksw.rdfunit.elements.interfaces.PatternParameter;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Util functions that instantiate patterns from a QEF
 *
 * @author Dimitris Kontokostas
 *         Description
 * @since 9/23/13 11:09 AM
 * @version $Id: $Id
 */
public final class PatternUtils {

    private PatternUtils() {
    }


    /**
     * Takes a QEF and tries to instantiate all defined patterns in that QEF
     *
     * @param queryFactory the query factory
     * @return the collection
     */
    public static Collection<Pattern> instantiatePatternsFromModel(QueryExecutionFactory queryFactory) {

        final String sparqlSelectPatterns = org.aksw.rdfunit.services.PrefixNSService.getSparqlPrefixDecl() +
                "SELECT distinct ?sparqlPattern WHERE { " +
                " ?sparqlPattern a rut:Pattern . }";

        Collection<String> patternURIs = new ArrayList<>();


        QueryExecution qe = null;
        try {
            qe = queryFactory.createQueryExecution(sparqlSelectPatterns);
            ResultSet results = qe.execSelect();

            while (results.hasNext()) {
                QuerySolution qs = results.next();

                patternURIs.add(qs.get("sparqlPattern").toString());
            }
        } finally {
            if (qe != null) {
                qe.close();
            }
        }

        Collection<Pattern> patterns = new ArrayList<>();
        for (String patternURI : patternURIs) {
            patterns.add(getPattern(queryFactory, patternURI));
        }

        return patterns;
    }

    /**
     * Given a QueryExecutionFactory and a pattern URI, it instantiates a new Pattern
     *
     * @param queryFactory the query factory
     * @param patternURI   the pattern uRI
     * @return the pattern object
     */
    public static Pattern getPattern(QueryExecutionFactory queryFactory, String patternURI) {
        final String sparqlSelectPattern = org.aksw.rdfunit.services.PrefixNSService.getSparqlPrefixDecl() +
                "SELECT distinct ?sparqlPattern ?id ?desc ?sparql ?sparqlPrev ?variable WHERE { " +
                "  <" + patternURI + "> a rut:Pattern ; " +
                "  dcterms:identifier ?id ; " +
                "  dcterms:description ?desc ; " +
                "  rut:sparqlWherePattern ?sparql ; " +
                "  rut:sparqlPrevalencePattern ?sparqlPrev ; " +
                "} ORDER BY ?sparqlPattern";

        Pattern pattern = null;
        QueryExecution qe = null;
        try {
            qe = queryFactory.createQueryExecution(sparqlSelectPattern);
            ResultSet results = qe.execSelect();

            if (results.hasNext()) {
                QuerySolution qs = results.next();

                PatternImpl.Builder patternBuilder = new PatternImpl.Builder();

                patternBuilder.setElement(ResourceFactory.createResource(patternURI));
                patternBuilder.setId(qs.get("id").toString());
                patternBuilder.setDescription(qs.get("desc").toString());
                patternBuilder.setSparqlWherePattern(qs.get("sparql").toString());
                patternBuilder.setSparqlPatternPrevalence(qs.get("sparqlPrev").toString());

                // Get pattern parameters
                patternBuilder.setParameters(getPatternParameters(queryFactory, patternURI));

                // Get annotations from TAG URI
                patternBuilder.setAnnotations(SparqlUtils.getResultAnnotations(queryFactory, patternURI));

                pattern = patternBuilder.build();

                // if not valid OR if multiple results returns something is wrong
                if (!pattern.isValid() || results.hasNext()) {
                    throw new IllegalArgumentException("Pattern not valid: " + pattern.getId());
                }
            }
        } finally {
            if (qe != null) {
                qe.close();
            }
        }

        return pattern;
    }

    /**
     * Instantiates all parameters defined in a Pattern
     *
     * @param queryFactory the QEF to query to
     * @param patternURI   the pattern URI
     * @return a list of pattern parameters for a specific pattern
     */
    private static Collection<PatternParameter> getPatternParameters(QueryExecutionFactory queryFactory, String patternURI) {

        final String sparqlSelectParameters = org.aksw.rdfunit.services.PrefixNSService.getSparqlPrefixDecl() +
                " SELECT distinct ?parameterURI ?id ?constraint ?constraintPattern WHERE { " +
                " %%PATTERN%%  rut:parameter ?parameterURI . " +
                " ?parameterURI a rut:Parameter . " +
                " ?parameterURI dcterms:identifier ?id . " +
                " OPTIONAL {?parameterURI rut:parameterConstraint ?constraint .}" +
                " OPTIONAL {?parameterURI rut:constraintPattern ?constraintPattern .}" +
                " } ";

        Collection<PatternParameter> parameters = new ArrayList<>();

        QueryExecution qe = null;
        try {
            qe = queryFactory.createQueryExecution(sparqlSelectParameters.replace("%%PATTERN%%", "<" + patternURI + ">"));
            ResultSet rs = qe.execSelect();

            while (rs.hasNext()) {

                QuerySolution parSol = rs.next();

                PatternParameterImpl.Builder patternParameterBuilder = new PatternParameterImpl.Builder();

                patternParameterBuilder.setElement(parSol.get("parameterURI").asResource());
                patternParameterBuilder.setID(parSol.get("id").toString());

                if (parSol.contains("constrain")) {
                    patternParameterBuilder.setPatternParameterConstraints(parSol.get("constrain").toString());
                }

                if (parSol.contains("constraintPattern")) {
                    patternParameterBuilder.setContraintPattern(parSol.get("constraintPattern").toString());
                }


                parameters.add(patternParameterBuilder.build());
            }
        } finally {
            if (qe != null) {
                qe.close();
            }
        }

        return parameters;
    }
}
