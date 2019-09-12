SELECT concept.concept_id,
       concept.concept_name,
       concept.domain_id,
       concept.vocabulary_id,
       concept.concept_class_id,
       concept.standard_concept,
       concept.concept_code,
       concept.valid_start_date,
       concept.valid_end_date,
       concept.invalid_reason,
       synonym.concept_synonym_name
FROM @vocab.concept
  LEFT OUTER JOIN @vocab.concept_synonym synonym
    ON concept.concept_id = synonym.concept_id
WHERE domain_id = 'Drug'
AND   vocabulary_id LIKE 'RxNorm%'
AND   concept_class_id = 'Ingredient'
AND   standard_concept = 'S'
AND   invalid_reason IS NULL
ORDER BY concept.concept_id,
         synonym.concept_synonym_name