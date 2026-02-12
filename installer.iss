; Script de Inno Setup para Sistema de Asistencia Biometrica
; Generado por Antigravity

[Setup]
AppId={{D35F6A1B-8E4B-4E3B-AC2D-F6D5E6B7C8A9}
AppName=Sistema de Asistencia Biometrica
AppVersion=1.0
AppPublisher=Softclass
DefaultDirName={autopf}\SistemaAsistenciaBiometrica
DefaultGroupName=Sistema de Asistencia Biometrica
AllowNoIcons=yes
CreateAppDir=yes
PrivilegesRequired=admin
OutputDir=dist\Installer
OutputBaseFilename=Instalador_Asistencia_Biometrica
Compression=lzma
SolidCompression=yes
SetupIconFile=d:\SOFTCLASS\Biometricos\DigitalPersona\DigitalPersonaDesktop\icon.ico
WizardStyle=modern

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "startup"; Description: "Ejecutar al iniciar Windows"; GroupDescription: "Opciones adicionales:"; Flags: unchecked

[Dirs]
; Conceder permisos de escritura a todos los usuarios en la carpeta del programa para la base de datos
Name: "{app}"; Permissions: users-modify

[Files]
; Copiar todos los archivos de la App Image de jpackage
Source: "d:\SOFTCLASS\Biometricos\DigitalPersona\DigitalPersonaDesktop\dist\DigitalPersonaApp\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Sistema de Asistencia Biometrica"; Filename: "{app}\DigitalPersonaApp.exe"; WorkingDir: "{app}"; IconFilename: "{app}\icon.ico"
Name: "{commondesktop}\Sistema de Asistencia Biometrica"; Filename: "{app}\DigitalPersonaApp.exe"; Tasks: desktopicon; WorkingDir: "{app}"; IconFilename: "{app}\icon.ico"
Name: "{group}\DEBUG - Abrir con Consola"; Filename: "{app}\run.bat"; WorkingDir: "{app}"
Name: "{userstartup}\Sistema de Asistencia Biometrica"; Filename: "{app}\DigitalPersonaApp.exe"; WorkingDir: "{app}"; Tasks: startup

[Run]
Filename: "{app}\DigitalPersonaApp.exe"; Description: "{cm:LaunchProgram,Sistema de Asistencia Biometrica}"; Flags: nowait postinstall skipifsilent
