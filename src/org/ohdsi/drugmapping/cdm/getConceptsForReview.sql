SELECT *
FROM @vocab.concept
WHERE invalid_reason IS NULL
AND   (   (    (domain_id = 'Drug')
           AND (vocabulary_id LIKE 'RxNorm%')
           AND (concept_class_id IN ('Ingredient', 'Clinical Drug', 'Clinical Drug Comp', 'Clinical Drug Form'))
           AND (standard_concept = 'S')
          )
       OR (    (domain_id = 'Drug')
           AND (concept_class_id = 'Dose Form')
          )
       OR (    (domain_id = 'Unit')
           AND (concept_class_id = 'Unit')
           AND (standard_concept = 'S')
          )
      );