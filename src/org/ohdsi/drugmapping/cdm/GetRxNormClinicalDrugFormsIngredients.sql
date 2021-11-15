SELECT DRUGFORM.CONCEPT_ID AS DRUGFORM_CONCEPT_ID,
       DRUGFORM.CONCEPT_NAME AS DRUGFORM_CONCEPT_NAME,
       DRUGFORM.DOMAIN_ID AS DRUGFORM_DOMAIN_ID,
       DRUGFORM.VOCABULARY_ID AS DRUGFORM_VOCABULARY_ID,
       DRUGFORM.CONCEPT_CLASS_ID AS DRUGFORM_CONCEPT_CLASS_ID,
       DRUGFORM.STANDARD_CONCEPT AS DRUGFORM_STANDARD_CONCEPT,
       DRUGFORM.CONCEPT_CODE AS DRUGFORM_CONCEPT_CODE,
       DRUGFORM.VALID_START_DATE AS DRUGFORM_VALID_START_DATE,
       DRUGFORM.VALID_END_DATE AS DRUGFORM_VALID_END_DATE,
       DRUGFORM.INVALID_REASON AS DRUGFORM_INVALID_REASON,
       FORM.CONCEPT_ID AS FORM_CONCEPT_ID,
       STRENGTH.INGREDIENT_CONCEPT_ID
FROM @vocab.CONCEPT DRUGFORM
  INNER JOIN @vocab.DRUG_STRENGTH STRENGTH
    ON STRENGTH.DRUG_CONCEPT_ID = DRUGFORM.CONCEPT_ID
  LEFT OUTER JOIN @vocab.CONCEPT_RELATIONSHIP HAS_FORM
    ON DRUGFORM.CONCEPT_ID = HAS_FORM.CONCEPT_ID_1
  LEFT OUTER JOIN @vocab.CONCEPT FORM
    ON HAS_FORM.CONCEPT_ID_2 = FORM.CONCEPT_ID
WHERE DRUGFORM.DOMAIN_ID = 'Drug'
AND   UPPER(DRUGFORM.VOCABULARY_ID) LIKE 'RXNORM%'
AND   DRUGFORM.CONCEPT_CLASS_ID = 'Clinical Drug Form'
AND   DRUGFORM.STANDARD_CONCEPT = 'S'
-- AND   DRUGFORM.INVALID_REASON IS NULL
AND   HAS_FORM.RELATIONSHIP_ID = 'RxNorm has dose form'
GROUP BY DRUGFORM.CONCEPT_ID,
         DRUGFORM.CONCEPT_NAME,
         DRUGFORM.DOMAIN_ID,
         DRUGFORM.VOCABULARY_ID,
         DRUGFORM.CONCEPT_CLASS_ID,
         DRUGFORM.STANDARD_CONCEPT,
         DRUGFORM.CONCEPT_CODE,
         DRUGFORM.VALID_START_DATE,
         DRUGFORM.VALID_END_DATE,
         DRUGFORM.INVALID_REASON,
         FORM.CONCEPT_ID,
         STRENGTH.INGREDIENT_CONCEPT_ID
ORDER BY DRUGFORM.CONCEPT_ID,
         FORM.CONCEPT_ID,
         STRENGTH.INGREDIENT_CONCEPT_ID