WITH atc_ingredient AS (
	SELECT atc.concept_code AS atc
	,      atc.concept_name AS atc_concept_name
	,      ingredient.concept_id AS ingredient_concept_id
	,      ingredient.concept_name AS ingredient_concept_name
	FROM @vocab.concept_ancestor atc_to_ingredient
	INNER JOIN @vocab.concept atc
		ON atc_to_ingredient.ancestor_concept_id = atc.concept_id
	INNER JOIN @vocab.concept ingredient
		ON atc_to_ingredient.descendant_concept_id = ingredient.concept_id
	WHERE atc.vocabulary_id = 'ATC'
	AND   ingredient.vocabulary_id = 'RxNorm'
	AND   ingredient.concept_class_id = 'Ingredient'
	AND   UPPER(atc.concept_name) = UPPER(ingredient.concept_name)
)
SELECT DISTINCT atc_ingredient.atc AS atc,
       drug_strength.ingredient_concept_id,
       ingredient.concept_name AS ingredient_name,
       CASE
           WHEN amount_value IS NULL
               THEN numerator_value
               ELSE amount_value
           END AS amount_value,
       CASE
           WHEN amount_unit_concept_id IS NULL
               THEN CONCAT(numerator_unit.concept_name,'/',denominator_unit.concept_name)
               ELSE amount_unit.concept_name
           END AS amount_unit,
       CASE
           WHEN amount_unit_concept_id IS NULL
               THEN NULL
               ELSE amount_unit.concept_id
           END AS amount_unit_concept_id,
       NULL AS form,
       drug.*
FROM @vocab.drug_strength
LEFT JOIN @vocab.concept ingredient
       ON drug_strength.ingredient_concept_id = ingredient.concept_id
LEFT JOIN atc_ingredient
       ON drug_strength.ingredient_concept_id = atc_ingredient.ingredient_concept_id
INNER JOIN @vocab.concept drug
       ON drug_strength.drug_concept_id = drug.concept_id
LEFT JOIN @vocab.concept_relationship
       ON concept_relationship.concept_id_1 = drug.concept_id
LEFT JOIN @vocab.concept amount_unit
       ON amount_unit_concept_id = amount_unit.concept_id
LEFT JOIN @vocab.concept numerator_unit
       ON numerator_unit_concept_id = numerator_unit.concept_id
LEFT JOIN @vocab.concept denominator_unit
       ON denominator_unit_concept_id = denominator_unit.concept_id
WHERE drug_strength.drug_concept_id IN (
          /* Drugs with one ingredient */
          SELECT drug_concept_id
          FROM @vocab.drug_strength
          WHERE invalid_reason IS NULL
          GROUP BY drug_concept_id
          HAVING COUNT(*) = 1
          )
      AND drug_strength.invalid_reason IS NULL
      AND drug.concept_class_id = 'Clinical Drug Comp'
      AND drug.standard_concept = 'S'
      AND drug.invalid_reason IS NULL
UNION ALL
SELECT atc_ingredient.atc AS atc,
       ingredient.concept_id AS ingredient_concept_id,
       ingredient.concept_name AS ingredient_name,
       NULL AS amount_value,
       NULL AS amount_unit,
       NULL AS amount_unit_concept_id,
       form.concept_name AS form,
       drug.*
FROM @vocab.concept drug
LEFT JOIN @vocab.concept_relationship cr1
       ON cr1.concept_id_1 = drug.concept_id
LEFT JOIN @vocab.concept form
       ON form.concept_id = cr1.concept_id_2

LEFT JOIN @vocab.concept_relationship cr2
       ON cr2.concept_id_1 = drug.concept_id
LEFT JOIN @vocab.concept ingredient
       ON ingredient.concept_id = cr2.concept_id_2

LEFT JOIN atc_ingredient
       ON ingredient.concept_id = atc_ingredient.ingredient_concept_id
WHERE drug.concept_id IN (
          SELECT concept_id_1
          FROM @vocab.concept_relationship r
          LEFT JOIN @vocab.concept drug
                 ON r.concept_id_1 = drug.concept_id
          WHERE relationship_id = 'RxNorm has ing'
          AND   drug.concept_class_id = 'Clinical Drug Form'
          GROUP BY r.concept_id_1
          HAVING COUNT(*) = 1
      )
      AND cr1.relationship_id = 'RxNorm has dose form'
      AND cr2.relationship_id = 'RxNorm has ing'
UNION ALL
SELECT atc_ingredient.atc AS atc,
       drug_strength.ingredient_concept_id,
       ingredient.concept_name AS ingredient_name,
       CASE
           WHEN amount_value IS NULL
               THEN numerator_value
               ELSE amount_value
           END AS amount_value,
       CASE
           WHEN amount_unit_concept_id IS NULL
               THEN concat(numerator_unit.concept_name,'/',denominator_unit.concept_name)
               ELSE amount_unit.concept_name
           END AS amount_unit,
       CASE
           WHEN amount_unit_concept_id IS NULL
               THEN NULL
               ELSE amount_unit.concept_id
           END AS amount_unit_concept_id,
       form.concept_name AS form,
       drug.*
FROM @vocab.drug_strength
LEFT JOIN @vocab.concept ingredient
       ON drug_strength.ingredient_concept_id = ingredient.concept_id
LEFT JOIN atc_ingredient
       ON drug_strength.ingredient_concept_id = atc_ingredient.ingredient_concept_id
INNER JOIN @vocab.concept drug
       ON drug_strength.drug_concept_id = drug.concept_id
LEFT JOIN @vocab.concept_relationship
       ON concept_relationship.concept_id_1 = drug.concept_id
LEFT JOIN @vocab.concept form
       ON form.concept_id = concept_relationship.concept_id_2
LEFT JOIN @vocab.concept amount_unit
       ON amount_unit_concept_id = amount_unit.concept_id
LEFT JOIN @vocab.concept numerator_unit
       ON numerator_unit_concept_id = numerator_unit.concept_id
LEFT JOIN @vocab.concept denominator_unit
       ON denominator_unit_concept_id = denominator_unit.concept_id
WHERE drug_strength.drug_concept_id IN (
          /* Drugs with one ingredient */
          SELECT drug_concept_id
          FROM @vocab.drug_strength
          WHERE invalid_reason IS NULL
          GROUP BY drug_concept_id
          HAVING COUNT(*) = 1
      )
      AND concept_relationship.relationship_id = 'RxNorm has dose form'
      AND drug_strength.invalid_reason IS NULL
      AND drug.concept_class_id = 'Clinical Drug'
      AND drug.standard_concept = 'S'
      AND drug.invalid_reason IS NULL