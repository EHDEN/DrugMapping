SELECT concept.*
FROM @vocab.concept
WHERE UPPER(concept_name) LIKE '%@name%'
AND domain_id = 'Drug'
AND vocabulary_id like 'RxNorm%'
AND concept_class_id = 'Ingredient'
AND standard_concept = 'S'