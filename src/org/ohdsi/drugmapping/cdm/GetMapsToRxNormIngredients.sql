SELECT source.concept_name AS source_name
,      concept.concept_id
,      synonym.concept_synonym_name
FROM @vocab.concept_relationship relationship
LEFT OUTER JOIN @vocab.concept source
ON relationship.concept_id_1 = source.concept_id
LEFT OUTER JOIN @vocab.concept concept
ON relationship.concept_id_2 = concept.concept_id
LEFT OUTER JOIN @vocab.concept_synonym synonym
ON source.concept_id = synonym.concept_id
WHERE relationship.relationship_id = 'Maps to'
AND   source.domain_id = 'Drug'
AND   concept.domain_id = 'Drug'
AND   concept.vocabulary_id LIKE 'RxNorm%'
AND   concept.concept_class_id = 'Ingredient'
AND   concept.standard_concept = 'S'
AND   UPPER(source.concept_name) <> UPPER(concept.concept_name)