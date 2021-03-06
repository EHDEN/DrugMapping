SELECT drug.concept_id AS drug_concept_id,
       drug.concept_name AS drug_concept_name,
       drug.domain_id AS drug_domain_id,
       drug.vocabulary_id AS drug_vocabulary_id,
       drug.concept_class_id AS drug_concept_class_id,
       drug.standard_concept AS drug_standard_concept,
       drug.concept_code AS drug_concept_code,
       drug.valid_start_date AS drug_valid_start_date,
       drug.valid_end_date AS drug_valid_end_date,
       drug.invalid_reason AS drug_invalid_reason,
       form.concept_id AS form_concept_id,
       strength.amount_value,
       strength.amount_unit_concept_id,
       amount_unit.concept_name AS amount_unit_concept_name,
       amount_unit.domain_id AS amount_unit_domain_id,
       amount_unit.vocabulary_id AS amount_unit_vocabulary_id,
       amount_unit.concept_class_id AS amount_unit_concept_class_id,
       amount_unit.standard_concept AS amount_unit_standard_concept,
       amount_unit.concept_code AS amount_unit_concept_code,
       amount_unit.valid_start_date AS amount_unit_valid_start_date,
       amount_unit.valid_end_date AS amount_unit_valid_end_date,
       amount_unit.invalid_reason AS amount_unit_invalid_reason,
       strength.numerator_value,
       strength.numerator_unit_concept_id,
       numerator_unit.concept_name AS numerator_unit_concept_name,
       numerator_unit.domain_id AS numerator_unit_domain_id,
       numerator_unit.vocabulary_id AS numerator_unit_vocabulary_id,
       numerator_unit.concept_class_id AS numerator_unit_concept_class_id,
       numerator_unit.standard_concept AS numerator_unit_standard_concept,
       numerator_unit.concept_code AS numerator_unit_concept_code,
       numerator_unit.valid_start_date AS numerator_unit_valid_start_date,
       numerator_unit.valid_end_date AS numerator_unit_valid_end_date,
       numerator_unit.invalid_reason AS numerator_unit_invalid_reason,
       strength.denominator_value,
       strength.denominator_unit_concept_id,
       denominator_unit.concept_name AS denominator_unit_concept_name,
       denominator_unit.domain_id AS denominator_unit_domain_id,
       denominator_unit.vocabulary_id AS denominator_unit_vocabulary_id,
       denominator_unit.concept_class_id AS denominator_unit_concept_class_id,
       denominator_unit.standard_concept AS denominator_unit_standard_concept,
       denominator_unit.concept_code AS denominator_unit_concept_code,
       denominator_unit.valid_start_date AS denominator_unit_valid_start_date,
       denominator_unit.valid_end_date AS denominator_unit_valid_end_date,
       denominator_unit.invalid_reason AS denominator_unit_invalid_reason,
       strength.box_size,
       strength.ingredient_concept_id
FROM @vocab.concept drug
  INNER JOIN @vocab.drug_strength strength
    ON strength.drug_concept_id = drug.concept_id
  LEFT OUTER JOIN @vocab.concept_relationship has_form
    ON drug.concept_id = has_form.concept_id_1
  LEFT OUTER JOIN @vocab.concept form
    ON has_form.concept_id_2 = form.concept_id
  LEFT OUTER JOIN @vocab.concept amount_unit
    ON strength.amount_unit_concept_id = amount_unit.concept_id
  LEFT OUTER JOIN @vocab.concept numerator_unit
    ON strength.numerator_unit_concept_id = numerator_unit.concept_id
  LEFT OUTER JOIN @vocab.concept denominator_unit
    ON strength.denominator_unit_concept_id = denominator_unit.concept_id
WHERE drug.domain_id = 'Drug'
AND   drug.vocabulary_id LIKE 'RxNorm%'
AND   drug.concept_class_id = 'Clinical Drug'
AND   drug.standard_concept = 'S'
-- AND   drug.invalid_reason IS NULL
AND   has_form.relationship_id = 'RxNorm has dose form'
GROUP BY drug.concept_id,
         drug.concept_name,
         drug.domain_id,
         drug.vocabulary_id,
         drug.concept_class_id,
         drug.standard_concept,
         drug.concept_code,
         drug.valid_start_date,
         drug.valid_end_date,
         drug.invalid_reason,
         form.concept_id,
         strength.amount_value,
         strength.amount_unit_concept_id,
         amount_unit.concept_name,
         amount_unit.domain_id,
         amount_unit.vocabulary_id,
         amount_unit.concept_class_id,
         amount_unit.standard_concept,
         amount_unit.concept_code,
         amount_unit.valid_start_date,
         amount_unit.valid_end_date,
         amount_unit.invalid_reason,
         strength.numerator_value,
         strength.numerator_unit_concept_id,
         numerator_unit.concept_name,
         numerator_unit.domain_id,
         numerator_unit.vocabulary_id,
         numerator_unit.concept_class_id,
         numerator_unit.standard_concept,
         numerator_unit.concept_code,
         numerator_unit.valid_start_date,
         numerator_unit.valid_end_date,
         numerator_unit.invalid_reason,
         strength.denominator_value,
         strength.denominator_unit_concept_id,
         denominator_unit.concept_name,
         denominator_unit.domain_id,
         denominator_unit.vocabulary_id,
         denominator_unit.concept_class_id,
         denominator_unit.standard_concept,
         denominator_unit.concept_code,
         denominator_unit.valid_start_date,
         denominator_unit.valid_end_date,
         denominator_unit.invalid_reason,
         strength.box_size,
         strength.ingredient_concept_id
ORDER BY drug.concept_id,
         form.concept_id,
         strength.ingredient_concept_id