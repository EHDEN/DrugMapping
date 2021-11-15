SELECT ingredient.concept_id,
       ingredient.concept_name,
       ingredient.domain_id,
       ingredient.vocabulary_id,
       ingredient.concept_class_id,
       ingredient.standard_concept,
       ingredient.concept_code,
       ingredient.valid_start_date,
       ingredient.valid_end_date,
       ingredient.invalid_reason,
       synonym.concept_synonym_name
FROM @vocab.concept ingredient
  LEFT OUTER JOIN @vocab.concept_synonym synonym
    ON ingredient.concept_id = synonym.concept_id
WHERE ingredient.concept_id = 0
OR (ingredient.domain_id = 'Drug'
    AND   ingredient.vocabulary_id ILIKE 'RxNorm%'
    AND   ingredient.concept_class_id = 'Ingredient'
    AND   ingredient.standard_concept = 'S'
--    AND   ingredient.invalid_reason IS NULL
-- Exclude orphan ingredients:
--    AND   ingredient.concept_id IN (
--            SELECT DISTINCT ingredient_concept_id
--            FROM @vocab.drug_strength strength
--            LEFT OUTER JOIN @vocab.concept drug
--                ON strength.drug_concept_id = drug.concept_id
--            LEFT OUTER JOIN @vocab.concept ingredient
--                ON strength.ingredient_concept_id = ingredient.concept_id
--            WHERE drug.domain_id = 'Drug'
--            AND   drug.vocabulary_id ILIKE 'RxNorm%'
--            AND   drug.concept_class_id IN ('Clinical Drug', 'Clinical Drug Comp', 'Clinical Drug Form')
--            AND   drug.standard_concept = 'S'
--            AND   drug.invalid_reason IS NULL
--            AND   ingredient.domain_id = 'Drug'
--            AND   ingredient.vocabulary_id ILIKE 'RxNorm%'
--            AND   ingredient.concept_class_id = 'Ingredient'
--            AND   ingredient.standard_concept = 'S'
--            AND   ingredient.invalid_reason IS NULL
--        )
   )
ORDER BY ingredient.concept_id,
         synonym.concept_synonym_name