SELECT drug.concept_id AS drug_concept_id,
       drug.concept_name AS drug_concept_name,
       mapsto.concept_id AS mapsto_concept_id,
       synonym.concept_synonym_name AS drug_synonym_name
FROM @vocab.concept_relationship relationship
  LEFT OUTER JOIN @vocab.concept drug
    ON relationship.concept_id_1 = drug.concept_id
  LEFT OUTER JOIN @vocab.concept mapsto
    ON relationship.concept_id_2 = mapsto.concept_id
  LEFT OUTER JOIN @vocab.concept_synonym synonym
    ON drug.concept_id = synonym.concept_id
WHERE relationship.relationship_id = 'Maps to'
AND   drug.domain_id = 'Drug'
AND   mapsto.domain_id = 'Drug'
AND   mapsto.vocabulary_id LIKE 'RxNorm%'
AND   mapsto.concept_class_id = 'Ingredient'
AND   mapsto.standard_concept = 'S'
AND   mapsto.invalid_reason IS NULL
AND   UPPER(drug.concept_name) <> UPPER(mapsto.concept_name)