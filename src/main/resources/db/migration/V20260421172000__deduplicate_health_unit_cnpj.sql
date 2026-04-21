-- 1) Redireciona users para a health_unit canônica (menor id por cnpj)
UPDATE users u
SET health_unit_id = map.keep_id
FROM (
    SELECT hu.id AS old_id,
           canon.keep_id
    FROM health_unit hu
    JOIN (
        SELECT cnpj, MIN(id) AS keep_id
        FROM health_unit
        WHERE cnpj IS NOT NULL
        GROUP BY cnpj
    ) canon
      ON canon.cnpj = hu.cnpj
    WHERE hu.id <> canon.keep_id
) map
WHERE u.health_unit_id = map.old_id;

-- 2) Remove health_units duplicadas, mantendo a de menor id por cnpj
DELETE FROM health_unit
WHERE id IN (
    SELECT hu.id
    FROM health_unit hu
    JOIN (
        SELECT cnpj, MIN(id) AS keep_id
        FROM health_unit
        WHERE cnpj IS NOT NULL
        GROUP BY cnpj
    ) canon
      ON canon.cnpj = hu.cnpj
    WHERE hu.id <> canon.keep_id
);

-- 3) Remove endereços órfãos, ou seja, que não são usados nem por health_unit nem por users
DELETE FROM addresses a
WHERE NOT EXISTS (
    SELECT 1
    FROM health_unit hu
    WHERE hu.address_id = a.id
)
AND NOT EXISTS (
    SELECT 1
    FROM users u
    WHERE u.address_id = a.id
);