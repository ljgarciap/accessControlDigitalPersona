# Sistema de Asistencia Biométrica - DigitalPersona Desktop

Este proyecto es una aplicación de escritorio Java para la gestión de asistencia mediante lectores biométricos DigitalPersona.

## Guía de Compilación y Despliegue

### 1. Construcción del Proyecto (Maven)
Para generar los archivos JAR actualizados:
```powershell
mvn clean package -U
```
Esto generará los JARs en la carpeta `target/`.

### 2. Actualización de la Distribución
Los JARs deben ser copiados a la carpeta de distribución para que el instalador los incluya:
- De: `target/*.jar`
- A: `dist/DigitalPersonaApp/app/`

### 3. Generación del Instalador (Inno Setup)
Para generar el archivo `Instalador_Asistencia_Biometrica.exe`:
1. Abrir `installer.iss` en **Inno Setup Compiler**.
2. Presionar **F9** para compilar.
3. El instalador se generará en `dist/Installer/`.

## Bitácora de Avances

### 2026-03-16
- **Actualización de Inicializador**: Se realizó un ajuste en el llamado del inicializador en el código fuente.
- **Sincronización de Distribución**: Se copiaron los nuevos JARs generados por Maven a la carpeta `dist/DigitalPersonaApp/app`.
- **Optimización de Instalador**: Se actualizaron las rutas en `installer.iss` para usar rutas relativas y corregir la ubicación del icono, eliminando dependencias de carpetas absolutas externas.
- **Creación de Documentación**: Se implementó este `README.md` para el seguimiento del proyecto.

---
*Nota: Este log se actualiza con cada cambio significativo en la arquitectura o el flujo de despliegue.*
