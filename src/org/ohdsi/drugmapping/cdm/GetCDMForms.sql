SELECT DISTINCT FORM.*
FROM @vocab.CONCEPT FORM
WHERE DOMAIN_ID = 'Drug'
AND   CONCEPT_CLASS_ID = 'Dose Form'
ORDER BY CONCEPT_ID