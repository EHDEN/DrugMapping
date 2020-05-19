SELECT ctd.concept_code AS casnr,
       ingredient.concept_id
FROM @vocab.concept ctd
LEFT OUTER JOIN @vocab.concept_relationship mapsto
    ON ctd.concept_id = mapsto.concept_id_1
INNER JOIN @vocab.concept ingredient
    ON mapsto.concept_id_2 = ingredient.concept_id
WHERE ctd.vocabulary_id = 'CTD'
AND   ctd.invalid_reason IS NULL
AND   ingredient.domain_id = 'Drug'
AND   ingredient.vocabulary_id LIKE 'RxNorm%'
AND   ingredient.concept_class_id = 'Ingredient'
AND   ingredient.standard_concept = 'S'
AND   ingredient.invalid_reason IS NULL
ORDER BY ingredient.concept_id ASC;