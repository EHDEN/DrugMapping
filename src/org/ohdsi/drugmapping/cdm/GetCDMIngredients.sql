SELECT concept.*
FROM @vocab.concept
WHERE domain_id = 'Drug'
AND vocabulary_id LIKE 'RxNorm%'
AND concept_class_id = 'Ingredient'
AND standard_concept = 'S'