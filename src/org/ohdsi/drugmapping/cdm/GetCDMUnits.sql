WITH units AS (
    SELECT DISTINCT amount_unit.*
    FROM @vocab.drug_strength
      LEFT OUTER JOIN @vocab.concept amount_unit
        ON amount_unit_concept_id = concept_id
  UNION ALL
    SELECT DISTINCT numerator_unit.*
    FROM @vocab.drug_strength
      LEFT OUTER JOIN @vocab.concept numerator_unit
        ON numerator_unit_concept_id = concept_id
  UNION ALL
    SELECT DISTINCT denominator_unit.*
    FROM @vocab.drug_strength
      LEFT OUTER JOIN @vocab.concept denominator_unit
        ON denominator_unit_concept_id = concept_id
)
SELECT *
FROM units
WHERE domain_id = 'Unit'
AND   concept_class_id = 'Unit'
AND   standard_concept = 'S'
AND   invalid_reason is null
ORDER BY concept_name