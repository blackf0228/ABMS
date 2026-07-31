# ABMS 再起動手順

この手順は、Windows再起動後、またはABMSを一度停止した後に、再び画面を使える状態に戻すためのものです。

## 1. 作業フォルダへ移動

PowerShellを開き、ABMSフォルダへ移動します。

```powershell
cd "C:\Users\black\Documents\Hello World\abms"
```

## 2. Docker Desktopを起動

Docker Desktopを開き、画面上でDockerが起動済みになるまで待ちます。

確認したい場合は、下記を実行します。

```powershell
docker info
```

エラーが出ずに情報が表示されればOKです。

## 3. PostgreSQLを起動

ABMSフォルダで下記を実行します。

```powershell
.\scripts\start-database.cmd
```

正常な場合、`abms-postgres` が起動します。

DB接続情報は下記です。

```text
Host: localhost
Port: 5432
Database: abms
User: abms
Password: abms_password
```

## 4. Backendを起動

別のPowerShellを開き、ABMSフォルダへ移動してから下記を実行します。

```powershell
cd "C:\Users\black\Documents\Hello World\abms"
.\scripts\start-backend.cmd
```

Backend URL:

```text
http://localhost:8080
```

確認用API:

```text
http://localhost:8080/api/hello
```

期待される結果:

```json
{
  "message": "Hello ABA"
}
```

## 5. Frontendを起動

さらに別のPowerShellを開き、ABMSフォルダへ移動してから下記を実行します。

```powershell
cd "C:\Users\black\Documents\Hello World\abms"
.\scripts\start-frontend.cmd
```

Frontend URL:

```text
http://localhost:4200
```

ブラウザで上記URLを開き、ABMSの画面が表示されれば再起動完了です。

## 5-1. コード変更後の再起動

JavaまたはAngularのソースを変更した場合は、DBを起動したままBackendとFrontendだけを再起動します。

1. Backendを起動しているPowerShellで `Ctrl + C` を押します。
2. Frontendを起動しているPowerShellで `Ctrl + C` を押します。
3. Backend用PowerShellで、下記を再実行します。

```powershell
cd "C:\Users\black\Documents\Hello World\abms"
.\scripts\start-backend.cmd
```

4. Frontend用PowerShellで、下記を再実行します。

```powershell
cd "C:\Users\black\Documents\Hello World\abms"
.\scripts\start-frontend.cmd
```

DBを停止していない場合、`start-database.cmd` の再実行は不要です。

## 6. CSVデータを再投入したい場合

売上、得意先、契約CSVを再投入したい場合のみ、下記を実行します。

```powershell
.\scripts\import-sales-csv-data.cmd
```

正常な場合、下記のような件数が表示されます。

```text
customer_masters     56
sales_imports        865
contracts            64
```

## 7. 停止方法

FrontendとBackendは、それぞれ起動しているPowerShellで `Ctrl + C` を押して停止します。

PostgreSQLを停止する場合は、ABMSフォルダで下記を実行します。

```powershell
docker compose down
```

データベースの中身も削除したい場合のみ、下記を実行します。

```powershell
docker compose down -v
```

通常の再起動では `-v` は使わないでください。DBデータが削除されます。

## 8. よくあるエラー

### Docker Engine is still not ready

Docker Desktopがまだ完全に起動していない状態です。

Docker Desktopを開き、Dockerが起動済みになるまで待ってから、もう一度実行してください。

```powershell
.\scripts\start-database.cmd
```

### permission denied while trying to connect to the docker API

Dockerの権限が反映されていない可能性があります。

一度Windowsからサインアウトするか、Windowsを再起動してください。

### 8080番ポートが使用中

Backendがすでに起動している可能性があります。

既存のBackend用PowerShellを確認し、不要であれば `Ctrl + C` で停止してから再起動してください。

### 4200番ポートが使用中

Frontendがすでに起動している可能性があります。

既存のFrontend用PowerShellを確認し、不要であれば `Ctrl + C` で停止してから再起動してください。
