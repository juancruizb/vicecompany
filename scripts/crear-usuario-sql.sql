/*
  Crea el usuario de SQL Server para ViceCompany, con el mínimo de permisos.
  Reemplaza al usuario "sa" en application-local.properties.

  Qué puede hacer este usuario:
    - LEER solo las columnas que usan las consultas de InconsistenciaRepository.java.
    - ACTUALIZAR solo Vice (UDF_CHAR20) y Cía (UDF_CHAR19) de Request_Additional_Fields_1.
    - Nada más: no puede borrar, insertar, cambiar estructura ni leer otras tablas.

  Cómo usarlo (en el servidor, con SQL Server Management Studio y un usuario administrador):
    1. Cambiar CAMBIAR_CONTRASENA (abajo) por una contraseña fuerte. NO guardar este archivo
       con la contraseña real ni subirlo a GitHub.
    2. Revisar que la base se llame "servicedesk" y que las tablas estén en el esquema "dbo".
    3. Ejecutar todo el script (F5). Se puede ejecutar más de una vez: no duplica nada.
    4. Ejecutar la sección "Verificación" al final.
    5. En application-local.properties, cambiar:
         spring.datasource.username=vicecompany
         spring.datasource.password=<la contraseña del paso 1>
       Reiniciar ViceCompany y comprobar que /inconsistencias carga igual que antes.

  Para deshacerlo: ver la sección "Eliminar" al final.
*/

-- 1. Login (a nivel del servidor SQL)
USE master;
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'vicecompany')
    CREATE LOGIN vicecompany
        WITH PASSWORD = N'CAMBIAR_CONTRASENA',
             DEFAULT_DATABASE = servicedesk,
             CHECK_POLICY = ON,
             CHECK_EXPIRATION = OFF;  -- OFF: si la contraseña vence, la sincronización se detiene.
GO

-- 2. Usuario dentro de la base servicedesk (sin ningún rol: solo los permisos de abajo)
USE servicedesk;
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'vicecompany')
    CREATE USER vicecompany FOR LOGIN vicecompany WITH DEFAULT_SCHEMA = dbo;
GO

-- 3. Lectura: solo las columnas que usan las consultas
GRANT SELECT ON dbo.WorkOrder (WORKORDERID, REQUESTERID, CREATEDTIME)                 TO vicecompany;
GRANT SELECT ON dbo.SDUser (USERID)                                                   TO vicecompany;
GRANT SELECT ON dbo.AaaUser (USER_ID, FIRST_NAME)                                     TO vicecompany;
GRANT SELECT ON dbo.UserAdditionalFields (INSTANCE_ID, UDF_CHAR2, UDF_CHAR1)          TO vicecompany;
GRANT SELECT ON dbo.Request_Additional_Fields_1 (WORKORDERID, UDF_CHAR20, UDF_CHAR19) TO vicecompany;
GO

-- 4. Escritura: solo Vice (UDF_CHAR20) y Cía (UDF_CHAR19) de los tickets
GRANT UPDATE ON dbo.Request_Additional_Fields_1 (UDF_CHAR20, UDF_CHAR19) TO vicecompany;
GO

/* ---------------------------------------------------------------------------
   Verificación (ejecutar después; no cambia datos)
   --------------------------------------------------------------------------- */
-- Permisos asignados: deben salir 5 SELECT y 1 UPDATE, con sus columnas.
SELECT pr.name AS usuario, pe.permission_name AS permiso, o.name AS tabla, c.name AS columna
  FROM sys.database_permissions pe
  JOIN sys.database_principals pr ON pe.grantee_principal_id = pr.principal_id
  JOIN sys.objects o ON pe.major_id = o.object_id
  LEFT JOIN sys.columns c ON c.object_id = pe.major_id AND c.column_id = pe.minor_id
 WHERE pr.name = N'vicecompany'
 ORDER BY o.name, pe.permission_name, c.name;
GO

-- Probar como el usuario nuevo: la primera consulta debe funcionar, la segunda debe fallar
-- con "The SELECT permission was denied" (no puede leer otras columnas).
EXECUTE AS USER = N'vicecompany';
SELECT TOP 5 WORKORDERID, UDF_CHAR20, UDF_CHAR19 FROM dbo.Request_Additional_Fields_1;
SELECT TOP 1 * FROM dbo.WorkOrder;
REVERT;
GO

/* ---------------------------------------------------------------------------
   Eliminar (solo si se quiere deshacer; volver antes a "sa" en las propiedades)
   ---------------------------------------------------------------------------
USE servicedesk;
DROP USER vicecompany;
USE master;
DROP LOGIN vicecompany;
*/
