# Java → AWS Bedrock POC

This project is a small **Java (Spring Boot)** service that shows how to call **Amazon Bedrock** from a Java application (the same pattern you can run on EC2).

- **Input**: HTTP POST request with a `prompt` string.
- **Action**: Service calls **Amazon Bedrock (Claude 3 Haiku)** using the **AWS SDK for Java v2**.
- **Output**: Returns the **raw JSON response** from Bedrock to the caller.

---

## 1. Tech Stack

- **Language**: Java 17  
- **Framework**: Spring Boot  
- **Build Tool**: Gradle  
- **Cloud**: AWS – Amazon Bedrock  
- **SDK**: AWS SDK for Java v2 (`bedrockruntime`)

The code is intentionally simple so it is easy to integrate into your existing Java / EC2 setup.

---

## 2. How the flow works

1. **Client → Java API**
Client Request  
      │  
      ▼  
Spring Boot Controller  
      │  
      ▼  
BedrockService  
      │  
      ▼  
AWS SDK (BedrockRuntimeClient)  
      │  
      ▼  
Amazon Bedrock  
      │  
      ▼  
Claude 3 Haiku Model  
      │  
      ▼  
JSON Response

   - Client calls:
     - `POST /api/bedrock/chat`
     - Body:
       ```json
       {
         "prompt": "Explain AWS Bedrock in 2 sentences."
       }
       ```

2. **Controller layer**
   - `BedrockController` receives the request.
   - It forwards the `prompt` string to `BedrockService.sendPrompt(...)`.

3. **Service layer – build Bedrock request**
   - `BedrockService` builds the request body in **Anthropic Claude 3** format:
     - `anthropic_version` – required version string.
     - `messages` – array with a single `"user"` message that contains your prompt.
     - `max_tokens`, `temperature` – basic generation settings.

4. **AWS Bedrock Runtime client**
   - A `BedrockRuntimeClient` bean is created in `BedrockConfig` with:
     - Region from `application.yml` (default `us-east-1`).
     - Credentials from **AWS Default Credentials Provider** (see next section).
   - The service calls `invokeModel` on this client with the JSON payload above.

5. **Response back to caller**
   - Bedrock returns a JSON response (Claude 3 format).
   - The service parses it with Jackson and immediately returns it to the HTTP client.

So the service is a thin layer between **your Java app** and **Bedrock**, focused on sending a prompt and returning the Bedrock JSON.

---

## 3. How authentication/credentials work

The code **does not** hard‑code any access keys.

### 3.1 Local development (already done for POC)

For local runs, credentials come from the AWS CLI configuration:

1. Run:
   ```bash
   aws configure
   ```
2. Enter:
   - Access key ID
   - Secret access key
   - Default region (e.g. `us-east-1`)
3. This writes to:
   - `~/.aws/credentials`
   - `~/.aws/config`

The Java app uses `DefaultCredentialsProvider`, which automatically reads those files and signs calls to Bedrock.

### 3.2 On EC2

On EC2 we should **not** store keys on disk. Instead:

1. Create an **IAM role** for EC2 (e.g. `ec2-bedrock-role`) with a policy that allows:
   - `bedrock:InvokeModel` on the required model(s), e.g. Claude 3 Haiku.
2. Attach that IAM role to the EC2 instance that runs the Java app.
3. When the app runs on EC2:
   - `DefaultCredentialsProvider` automatically fetches **temporary credentials** from the EC2 instance metadata service (the attached role).
   - No changes to application code or `application.yml` are required.

This is the standard and recommended pattern on AWS: **instance role → temporary credentials → SDK**.

---

## 4. Configuration

Main settings are in `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

bedrock:
  region: us-east-1
  modelId: anthropic.claude-3-haiku-20240307-v1:0
```

- `bedrock.region` – Bedrock region (e.g. `us-east-1`).
- `bedrock.modelId` – specific Bedrock model (here: Anthropic Claude 3 Haiku).

No secrets are stored in this file; it only controls which region/model to use.

---

## 5. Running the POC locally

From the project root:

```bash
./gradlew bootRun
```

Or with a local Gradle installation:

```bash
gradle bootRun
```

The service starts on:

- `http://localhost:8080`

### Test request

```bash
curl -X POST http://localhost:8080/api/bedrock/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Say hello from AWS Bedrock."}'
```

You will receive the JSON response from the Bedrock model.

---

## 6. Using this pattern in your EC2 Java application

To integrate into your existing Java application on EC2:

1. **Reuse the Bedrock service and config**
   - Copy the `BedrockConfig`, `BedrockService`, and DTO/controller (or adapt into your existing controller) into your app.

2. **Ensure IAM role permissions on EC2**
   - Attach an IAM role with `bedrock:InvokeModel` permissions.

3. **Adjust model/region if needed**
   - Change `bedrock.region` or `bedrock.modelId` to other Bedrock models if required.

The result is the same as this POC: your Java API can send a prompt to Bedrock and receive structured JSON back, ready for further processing in your system.

