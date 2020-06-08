SELECT drug.concept_id AS drug_concept_id,
       drug.concept_name AS drug_concept_name,
       equivalent.concept_id AS equivalent_concept_id,
       synonym.concept_synonym_name AS drug_synonym_name
FROM   @vocab.concept_relationship relationship
  LEFT OUTER JOIN @vocab.concept drug
    ON relationship.concept_id_1 = drug.concept_id
  LEFT OUTER JOIN @vocab.concept equivalent
    ON relationship.concept_id_2 = equivalent.concept_id
  LEFT OUTER JOIN @vocab.concept_synonym synonym
    ON drug.concept_id = synonym.concept_id
WHERE relationship.relationship_id ILIKE '%RxNorm eq'
AND   drug.domain_id = 'Drug'
AND   equivalent.domain_id = 'Drug'
AND   equivalent.vocabulary_id LIKE 'RxNorm%'
AND   equivalent.concept_class_id = 'Ingredient'
AND   equivalent.standard_concept = 'S'
AND   equivalent.invalid_reason IS NULL
ORDER BY drug.concept_id,
         synonym.concept_synonym_name,
         equivalent.concept_id