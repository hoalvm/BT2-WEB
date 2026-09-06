# Local Setup

This guide configures the existing `jpa-web-assignment-01` WAR on Windows with
JDK 24+, MySQL 8, and Tomcat 10.1. It does not create another project.

## 1. Install the prerequisites

Install these tools before cloning the repository:

- Git for Windows, including the `git` command on `PATH`.
- Oracle JDK or OpenJDK 24 or newer. This machine uses JDK 25.
- MySQL Server 8.x, MySQL Command Line Client, and `mysql.exe` on `PATH`.
- Apache Tomcat 10.1 using the Windows Service Installer. Keep the default
  service name `Tomcat10`.

Maven does not need to be installed separately because the repository includes
the Maven Wrapper (`mvnw.cmd`). During MySQL installation, configure the local
`root` password as `123456`, which is the classroom configuration used by this
project.

## 2. Clone the repository

Open PowerShell and replace the placeholder with the real repository URL:

```powershell
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd BT1-WEB
```

## 3. Configure Java and Tomcat

Set `JAVA_HOME` to JDK 24 or newer and `CATALINA_HOME` to Tomcat 10.1. Example
Machine-level configuration from Administrator PowerShell:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-25", "Machine")
[Environment]::SetEnvironmentVariable("CATALINA_HOME", "C:\Program Files\Apache Software Foundation\Tomcat 10.1", "Machine")
```

Replace the JDK folder in the example if your installed JDK 24+ uses another
version or location.

Open a new terminal and verify:

```powershell
git --version
java -version
javac -version
mysql --version
Get-Service MySQL*
Get-Service Tomcat10
$env:JAVA_HOME
$env:CATALINA_HOME
Test-Path "$env:JAVA_HOME\bin\javac.exe"
Test-Path "$env:CATALINA_HOME\bin\catalina.bat"
& "$env:CATALINA_HOME\bin\version.bat"
```

Both `Test-Path` commands must return `True`. At least one MySQL service and the
`Tomcat10` service must exist. The runner accepts JDK 24 or newer; Maven always
emits Java 24 bytecode.

## 4. Configure MySQL

The supplied local configuration expects MySQL at `localhost:3306` with user
`root`, password `123456`, and database `jpa_web`.

```powershell
mysql --host=localhost --port=3306 --user=root --password=123456 --execute="SELECT VERSION();"
```

`run.cmd` applies `database.sql` automatically. To apply it without deployment:

```powershell
mysql --host=localhost --port=3306 --user=root --password=123456 --default-character-set=utf8mb4 --execute="source database.sql"
```

Verify the schema and idempotent seed:

```powershell
mysql --host=localhost --port=3306 --user=root --password=123456 --execute="USE jpa_web; SHOW TABLES; SELECT COUNT(*) FROM products;"
```

The expected Product count on a new database is `10`. Reapplying the script does
not duplicate those records.

## 5. Create a Gmail App Password

1. Sign in to the Gmail sender account.
2. Enable 2-Step Verification in Google Account security settings.
3. Open App Passwords and create an App Password for this application.
4. Copy the generated 16-character password and store it without spaces.

Use the App Password, not the normal Gmail password. Never commit it.

## 6. Configure Gmail for Tomcat

Recommended option, from Administrator PowerShell:

```powershell
[Environment]::SetEnvironmentVariable("JPAWEB_MAIL_USERNAME", "sender@gmail.com", "Machine")
[Environment]::SetEnvironmentVariable("JPAWEB_MAIL_APP_PASSWORD", "your-16-character-app-password", "Machine")
Restart-Service Tomcat10
```

The authenticated Gmail username is also the From address. The runner reports
whether both variables exist but never prints their values.

### Tomcat JVM property alternative

If Machine environment variables are unavailable, open the service editor:

```powershell
& "$env:CATALINA_HOME\bin\tomcat10w.exe" //ES//Tomcat10
```

Add these Java options and restart `Tomcat10`:

```text
-Djpaweb.mail.username=sender@gmail.com
-Djpaweb.mail.appPassword=your-16-character-app-password
```

JVM properties take precedence over environment variables. Default SMTP settings:

```text
Host: smtp.gmail.com
Port: 587
Authentication: enabled
STARTTLS: enabled
```

## 7. Build and deploy

From Administrator PowerShell in the repository root:

```powershell
.\run.cmd
```

Build without database initialization or deployment:

```powershell
mvn clean package
```

Use `.\mvnw.cmd clean package` when Maven is not installed globally.

Application URL:

<http://localhost:8080/jpa-web-assignment-01/>

`run.cmd` is the one-command setup after the prerequisites and environment
variables are ready. It performs these steps:

1. Requests Administrator permission because it manages Windows services and a
   Machine-level upload directory.
2. Validates Git, JDK 24+, MySQL, Tomcat, `JAVA_HOME`, and `CATALINA_HOME`.
3. Creates `C:\ProgramData\JPAWeb\uploads` and sets `JPAWEB_UPLOAD_DIR` when no
   custom upload directory was provided.
4. Starts MySQL when needed and applies the idempotent `database.sql` script.
5. Runs `.\mvnw.cmd clean package` to compile, execute tests, and create the WAR.
6. Stops Tomcat, replaces only the `jpa-web-assignment-01` deployment, starts
   Tomcat, waits for HTTP 200, and opens the browser.

The script warns about missing Gmail variables but still builds and starts the
site. Registration and password-reset email require valid Gmail settings.

## 8. Verification after startup

```powershell
Invoke-WebRequest "http://localhost:8080/jpa-web-assignment-01/" -UseBasicParsing
mysql --host=localhost --port=3306 --user=root --password=123456 --execute="USE jpa_web; SHOW TABLES; SELECT COUNT(*) AS products FROM products;"
```

The HTTP response must be `200`, all five application tables must exist, and a
new database must contain ten seeded Products.

## 9. Troubleshooting

- Build logs and test reports: `target\surefire-reports`.
- Tomcat logs: `%CATALINA_HOME%\logs`.
- Port conflict: confirm no other application uses port `8080`.
- Database error: confirm a MySQL service is running and `root/123456` works.
- Upload error: confirm `JPAWEB_UPLOAD_DIR` exists and the Tomcat service account
  can write to it; `run.cmd` normally configures this automatically.

### Gmail

- Missing configuration: set both required variables or JVM properties, then
  restart Tomcat.
- Authentication failed: use an App Password and confirm 2-Step Verification.
- No email received: check Spam and verify the destination email address.
- Credentials stopped working: create a replacement App Password after Google
  account security changes, update the variable, and restart Tomcat.
- Build succeeds but email fails: builds do not contact Gmail. Test through
  registration or forgot-password after deployment.
