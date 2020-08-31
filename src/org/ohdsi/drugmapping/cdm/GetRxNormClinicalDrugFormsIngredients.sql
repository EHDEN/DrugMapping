SELECT drugform.concept_id AS drugform_concept_id,
       drugform.concept_name AS drugform_concept_name,
       drugform.domain_id AS drugform_domain_id,
       drugform.vocabulary_id AS drugform_vocabulary_id,
       drugform.concept_class_id AS drugform_concept_class_id,
       drugform.standard_concept AS drugform_standard_concept,
       drugform.concept_code AS drugform_concept_code,
       drugform.valid_start_date AS drugform_valid_start_date,
       drugform.valid_end_date AS drugform_valid_end_date,
       drugform.invalid_reason AS drugform_invalid_reason,
       form.concept_id AS form_concept_id,
       strength.ingredient_concept_id
FROM @vocab.concept drugform
  INNER JOIN @vocab.drug_strength strength
    ON strength.drug_concept_id = drugform.concept_id
  LEFT OUTER JOIN @vocab.concept_relationship has_form
    ON drugform.concept_id = has_form.concept_id_1
  LEFT OUTER JOIN @vocab.concept form
    ON has_form.concept_id_2 = form.concept_id
WHERE drugform.domain_id = 'Drug'
AND   drugform.vocabulary_id LIKE 'RxNorm%'
AND   drugform.concept_class_id = 'Clinical Drug Form'
AND   drugform.standard_concept = 'S'
-- AND   drugform.invalid_reason IS NULL
AND   has_form.relationship_id = 'RxNorm has dose form'
GROUP BY drugform.concept_id,
         drugform.concept_name,
         drugform.domain_id,
         drugform.vocabulary_id,
         drugform.concept_class_id,
         drugform.standard_concept,
         drugform.concept_code,
         drugform.valid_start_date,
         drugform.valid_end_date,
         drugform.invalid_reason,
         form.concept_id,
         strength.ingredient_concept_id
ORDER BY drugform.concept_id,
         form.concept_id,
         strength.ingredient_concept_id