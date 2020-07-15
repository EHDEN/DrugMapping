SELECT DISTINCT form.*
FROM @vocab.concept form
WHERE domain_id = 'Drug'
AND   concept_class_id = 'Dose Form'
ORDER BY concept_id