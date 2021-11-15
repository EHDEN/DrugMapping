SELECT INGREDIENT.CONCEPT_ID,
       INGREDIENT.CONCEPT_NAME,
       INGREDIENT.DOMAIN_ID,
       INGREDIENT.VOCABULARY_ID,
       INGREDIENT.CONCEPT_CLASS_ID,
       INGREDIENT.STANDARD_CONCEPT,
       INGREDIENT.CONCEPT_CODE,
       INGREDIENT.VALID_START_DATE,
       INGREDIENT.VALID_END_DATE,
       INGREDIENT.INVALID_REASON,
       CONCEPT_SYNONYM.CONCEPT_SYNONYM_NAME
FROM @vocab.CONCEPT INGREDIENT
  LEFT OUTER JOIN @vocab.CONCEPT_SYNONYM
    ON INGREDIENT.CONCEPT_ID = CONCEPT_SYNONYM.CONCEPT_ID
WHERE INGREDIENT.CONCEPT_ID = 0
OR (INGREDIENT.DOMAIN_ID = 'Drug'
    AND   UPPER(INGREDIENT.VOCABULARY_ID) LIKE 'RXNORM%'
    AND   INGREDIENT.CONCEPT_CLASS_ID = 'Ingredient'
    AND   INGREDIENT.STANDARD_CONCEPT = 'S'
--    AND   INGREDIENT.INVALID_REASON IS NULL
-- Exclude orphan ingredients:
--    AND   INGREDIENT.CONCEPT_ID IN (
--            SELECT DISTINCT INGREDIENT_CONCEPT_ID
--            FROM @vocab.DRUG_STRENGTH STRENGTH
--            LEFT OUTER JOIN @vocab.CONCEPT DRUG
--                ON STRENGTH.DRUG_CONCEPT_ID = DRUG.CONCEPT_ID
--            LEFT OUTER JOIN @vocab.CONCEPT INGREDIENT
--                ON STRENGTH.INGREDIENT_CONCEPT_ID = INGREDIENT.CONCEPT_ID
--            WHERE DRUG.DOMAIN_ID = 'Drug'
--            AND   UPPER(DRUG.VOCABULARY_ID) LIKE 'RXNORM%'
--            AND   DRUG.CONCEPT_CLASS_ID IN ('Clinical Drug', 'Clinical Drug Comp', 'Clinical Drug Form')
--            AND   DRUG.STANDARD_CONCEPT = 'S'
--            AND   DRUG.INVALID_REASON IS NULL
--            AND   INGREDIENT.DOMAIN_ID = 'Drug'
--            AND   UPPER(INGREDIENT.VOCABULARY_ID) LIKE 'RXNORM%'
--            AND   INGREDIENT.CONCEPT_CLASS_ID = 'Ingredient'
--            AND   INGREDIENT.STANDARD_CONCEPT = 'S'
--            AND   INGREDIENT.INVALID_REASON IS NULL
--        )
   )
ORDER BY INGREDIENT.CONCEPT_ID,
         CONCEPT_SYNONYM.CONCEPT_SYNONYM_NAME