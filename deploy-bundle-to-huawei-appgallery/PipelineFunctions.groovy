import groovy.json.JsonSlurperClassic

/**
 * Deploys AAB Bundle to Huawei App Gallery
 *
 * @param filePath AAB file Bundle.
 * @param clientId Client ID
 * @param clientSecret Client Secret
 * @param appId App ID
 */
def deployBundleToAppGallery(filePath, clientId, clientSecret, appId) {

    // https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agcapi-getstarted-0000001111845114
    // IMPORTANT: when creating api key: 'N/A' as Project

    def submitForReview = false // todo directly submitting for review
    def releaseType = 1 // 1: on the entire network, 3: by phase

    // 1) get access token
    def accessToken = getAppGalleryAccessToken(clientId, clientSecret)

    // 2) get upload url
    def uploadUrlResponse = getAppGalleryUploadUrl(clientId, accessToken, appId, releaseType, filePath)

    // 3) upload bundle
    def uploadBundleResponse = uploadHuaweiBundle(filePath, uploadUrlResponse.authCode, uploadUrlResponse.uploadUrl)

    def fileInfoList = uploadBundleResponse.result.UploadFileRsp.fileInfoList[0]

    // 4) update bundle info
    updateAppGalleryBundleInfo(accessToken, clientId, appId, releaseType, filePath, fileInfoList.fileDestUlr, fileInfoList.size)
}

private def getAppGalleryAccessToken(clientId, clientSecret) {

    def json = """
    {
        "grant_type" : "client_credentials",
        "client_id": "$clientId", 
        "client_secret": "$clientSecret"
    }
    """.stripMargin().stripIndent()

    def request = "curl -X POST https://connect-api.cloud.huawei.com/api/oauth2/v1/token " +
            "-H 'Content-Type: application/json' " +
            "-H 'cache-control: no-cache' " +
            "-d '$json'"

    def response = sh(script: request, returnStdout: true)

    println("$response")
    def jsonResponse = new JsonSlurperClassic().parseText(response)

    accessToken = jsonResponse.access_token

    return accessToken
}

private def getAppGalleryUploadUrl(clientId, accessToken, appId, releaseType, filePath) {

    def fileExtension = filePath.drop(filePath.lastIndexOf('.') + 1)

    def request = "curl -X GET 'https://connect-api.cloud.huawei.com/api/publish/v2/upload-url?appId=$appId&suffix=$fileExtension&releaseType=$releaseType' " +
            "-H 'Authorization: Bearer $accessToken' " +
            "-H 'Content-Type: application/json' " +
            "-H 'client_id: $clientId'"

    println("$request")

    def response = sh(script: request, returnStdout: true)
    println("$response")
    def jsonResponse = new JsonSlurperClassic().parseText(response)

    // uploadUrl = jsonResponse.uploadUrl
    // authCode = jsonResponse.authCode

    return jsonResponse
}

private def uploadHuaweiBundle(filePath, authCode, uploadUrl) {

    def request = "curl -X POST '$uploadUrl' " +
            "-H 'Accept: application/json' " +
            "-F 'authCode=$authCode' " +
            "-F 'fileCount=1' " +
            "-F 'parseType=1' " +
            "-F 'file=@${filePath}'"

    println("$request")

    def response = sh(script: request, returnStdout: true)
    println("$response")
    def jsonResponse = new JsonSlurperClassic().parseText(response)

    // def fileInfoList = jsonResponse.result.UploadFileRsp.fileInfoList[0]
    // fileDestUrl = fileInfoList.fileDestUlr
    // fileSize = fileInfoList.size

    return jsonResponse
}

private def updateAppGalleryBundleInfo(accessToken, clientId, appId, releaseType, filePath, fileDestUrl, fileSize) {

    def fileName = new File(filePath).name

    def json = """
    {
      "fileType": "5",
      "files": [
        {
          "fileName": "$fileName",
          "fileDestUrl": "$fileDestUrl",
          "size": "$fileSize"
        }
      ]
    }
    """.stripMargin().stripIndent()

    def request = "curl -X PUT 'https://connect-api.cloud.huawei.com/api/publish/v2/app-file-info?appId=$appId' " +
            "-H 'Authorization: Bearer $accessToken' " +
            "-H 'Content-Type: application/json' " +
            "-H 'releaseType: $releaseType' " +
            "-H 'client_id: $clientId' " +
            "-d '$json'"

    println("$request")

    def response = sh(script: request, returnStdout: true)
    println("$response")
    def jsonResponse = new JsonSlurperClassic().parseText(response)

    return jsonResponse
}

private def submitAppDirectly(accessToken, appId, clientId) {

    def request = "curl -X POST https://connect-api.cloud.huawei.com/api/publish/v2/app-submit?appid=$appId " +
            "-H 'Authorization: Bearer $accessToken' " +
            "-H 'client_id: $clientId'"

    println(request)

    def response = sh(script: request, returnStdout: true)
    println("$response")
    def jsonResponse = new JsonSlurperClassic().parseText(response)

    return jsonResponse
}
