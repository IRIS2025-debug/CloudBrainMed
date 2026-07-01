# CloudBrainMed API 接口文档

# CloudBrainMed API 接口文档



*源码校准版*

*版本：1\.0  *

*生成日期：2026\-06\-14  *

*依据：当前后端 Controller、DTO、VO、Entity、Service 与 Gateway 配置*



本版删除原文档中的项目架构、目录树、部署说明和未实现接口，仅保留前后端联调所需内容。



## 1\. 全局约定



### 1\.1 服务与路由



|服务|端口|主要路由|
|---|---|---|
|admin\-service|8000|/admin\-service/\*\*|
|ai\-service|8001|/ai\-service/**、/admin\-service/ml/**|
|auth\-service|8002|/api/auth/**、/auth\-service/**|
|doctor\-service|8003|/doctor\-service/**、/inspection\-doctor/**、/internal/doctor/**|
|patient\-service|8004|/api/patient/**、/patient\-service/**|
|payment\-service|8005|/api/payment/\*\*（当前网关未配置）|



Gateway 默认端口为 `80`，基地址为 `http://{gateway-host}`。



### 1\.2 请求与响应



|项目|约定|
|---|---|
|JSON 请求|`Content-Type: application/json`|
|文件上传|`Content-Type: multipart/form-data`|
|登录令牌|请求头 `token`；当前代码未使用 `Authorization: Bearer`|
|成功响应|`{"code":200,"msg":"成功","data":...}`|
|空数据|列表返回 `[]`；无业务数据通常返回 `null`|
|流式响应|AI 药品问答返回 `text/event-stream`，不使用统一 Result 包裹|
|日期|Date 使用 `yyyy-MM-dd`；DateTime 使用 ISO\-8601 格式|



### 1\.3 权限现状



*\[\!WARNING\]*

*当前 \`JwtAuthenticationFilter\`、\`RateLimitFilter\`、\`RequestLogFilter\` 尚无有效实现，管理员 MLOps 接口也未在 Controller 内校验 token。下文权限说明是必须落实的业务权限，不代表当前网关已经可靠执行。*



|范围|要求|当前状态|
|---|---|---|
|/api/auth/**、/auth\-service/patient/**|开放|可匿名访问|
|/api/patient/**、/patient\-service/**|患者本人或授权内部服务|部分接口手动解析 token，部分仍直接接收 patientId|
|/api/doctor/**、/api/ai/assistant/**|医生本人或记录所属医生|部分写接口已校验归属，部分仍缺失|
|/api/admin/\*\*|管理员|当前 MLOps Controller 未校验|
|/internal/\*\*|仅服务间调用|网关屏蔽；共享密钥校验|



### 1\.4 通用错误码



|code/HTTP|含义|说明|
|---|---|---|
|200|成功|AI 分析降级也可能 code=200，应继续检查 `data.status` 和 `fallback`|
|400|参数错误|Bean Validation 失败、重复注册等|
|401|未认证|患者登录账号或密码错误；网关统一 401 尚未落实|
|403|禁止访问|已定义但当前代码较少使用|
|404|未找到/隐藏内部接口|内部服务密钥错误主动返回 HTTP 404|
|500|服务或业务异常|当前 BusinessException 通常被包装为 code=500，HTTP 状态可能仍为 200|
|501|业务错误|常量已定义，当前未实际使用|



## 2\. API 模块说明



接口按业务模块组织。每个模块依次说明模块职责、路由范围、权限、数据模型和接口详情。



### 2\.1 认证模块



**模块职责：** 负责患者注册登录，以及医生、管理员统一登录和 JWT 签发。



**路由范围：** `/api/auth/**、/auth-service/patient/**`



**权限说明：** 登录和注册接口开放，无需 token。



**接口数量：** 3



#### 模块接口清单



|接口名称|请求方式|接口地址|
|---|---|---|
|医生/管理员登录|`POST`|`/api/auth/login`|
|患者注册|`POST`|`/auth-service/patient/register`|
|患者登录|`POST`|`/auth-service/patient/login`|



#### 模块数据模型



##### `PatientInfoVo`



|字段|类型|说明|
|---|---|---|
|patientId|String|患者ID|
|name|String|姓名|
|genderText|String|性别文本|
|phone|String|手机号|
|address|String|地址|
|age|Integer|年龄|
|birthday|String|出生日期，yyyy\-MM\-dd|
|createTime|DateTime|注册时间|



##### `AuthLoginVo`



|字段|类型|说明|
|---|---|---|
|patientId|String|患者ID|
|name|String|姓名|
|phone|String|手机号|
|token|String|JWT令牌|
|expireTime|Long|令牌过期时间配置值|



#### 2\.1\.1 公共认证



##### 2\.1\.1\.1 医生/管理员登录



|项目|内容|
|---|---|
|接口地址|`/api/auth/login`|
|请求方式|`POST`|
|请求头|JSON|
|权限说明|开放；roleType仅允许2医生、3管理员|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|phone|body|String|是|手机号（同一手机号可对应多种 doctorType）|
|password|body|String|是|当前实现按数据库「明文 OR md5(明文)」双重比对|
|roleType|body|Integer|是|2医生，3管理员|
|doctorType|body|Integer|否|医生子类型：1接诊 2检查 3检验；通常不传，由账号密码匹配到的医生记录返回，只有同手机号多医生账号需要精确定位时传入|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|token|String|JWT令牌（roleType=2 时内含 doctorType claim）|
|roleType|Integer|角色类型|
|doctorType|Integer|医生子类型（仅 roleType=2 返回）|



**请求示例**



```JSON
{
  "phone": "11111111111",
  "password": "123456",
  "roleType": 2
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "roleType": 2,
    "doctorType": 2
  }
}
```



**错误码：** 500 账号或密码错误；500 角色类型错误



**业务规则：** 账号不存在、密码不匹配或角色值非法时抛出业务异常；医生登录成功后返回医生表中的 `doctorType`，前端据此进入接诊、检查或检验工作台。



##### 2\.1\.1\.2 患者注册



|项目|内容|
|---|---|
|接口地址|`/auth-service/patient/register`|
|请求方式|`POST`|
|请求头|JSON|
|权限说明|开放|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|name|body|String|是|2\-20字符|
|gender|body|Integer|否|1男，2女|
|phone|body|String|是|中国大陆11位手机号|
|idCard|body|String|是|18位身份证号|
|address|body|String|否|地址|
|password|body|String|是|6\-20字符|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|PatientInfoVo|注册后的患者信息（见数据模型）|



**请求示例**



```JSON
{
  "name": "张三",
  "gender": 1,
  "phone": "13800000000",
  "idCard": "330102199105121234",
  "address": "杭州市",
  "password": "123456"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "patientId": "p1718000000000ABCD",
    "name": "张三",
    "genderText": "男",
    "phone": "13800000000",
    "address": "杭州市",
    "age": 35,
    "birthday": "1991-05-12",
    "createTime": "2026-06-01T10:20:30"
  }
}
```



**错误码：** 400 参数校验失败；400 手机号已注册；400 身份证已注册；500 注册失败



**业务规则：** 生日由身份证号计算；手机号、身份证号必须唯一；密码以MD5写入数据库。



##### 2\.1\.1\.3 患者登录



|项目|内容|
|---|---|
|接口地址|`/auth-service/patient/login`|
|请求方式|`POST`|
|请求头|JSON|
|权限说明|开放|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|phone|body|String|是|中国大陆11位手机号|
|password|body|String|是|登录密码|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|AuthLoginVo|患者身份与JWT（见数据模型）|



**请求示例**



```JSON
{
  "phone": "13800000000",
  "password": "123456"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "patientId": "p1718000000000ABCD",
    "name": "张三",
    "phone": "13800000000",
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expireTime": 86400000
  }
}
```



**错误码：** 400 参数校验失败；401 手机号或密码错误



**业务规则：** 密码按MD5比对；成功后更新最后登录时间。



### 2\.2 患者业务模块



**模块职责：** 负责患者个人资料、挂号、病历和处方查询，以及患者服务内部查询能力。



**路由范围：** `/api/patient/**、/patient-service/**`



**权限说明：** 原则上仅患者本人或授权内部服务可访问；部分接口仍需补充 token 与数据归属校验。



**接口数量：** 22



#### 模块接口清单



|接口名称|请求方式|接口地址|
|---|---|---|
|查询患者个人信息|`GET`|`/api/patient/profile/info`|
|更新患者基础信息|`PUT`|`/api/patient/profile/update`|
|上传患者头像|`POST`|`/api/patient/profile/avatar-upload`|
|修改患者手机号|`POST`|`/api/patient/profile/change-phone`|
|修改患者密码|`POST`|`/api/patient/profile/change-password`|
|身份证敏感操作二次验证|`POST`|`/api/patient/profile/verify-idcard`|
|查询患者挂号记录|`GET`|`/api/patient/profile/registers`|
|查询患者缴费记录|`GET`|`/api/patient/profile/payments`|
|查询个人中心聚合数据|`GET`|`/api/patient/profile/full`|
|按患者ID查询资料|`GET`|`/api/patient/info/{patientId}`|
|按患者ID查询挂号历史|`GET`|`/api/patient/register/history/{patientId}`|
|获取启用科室|`GET`|`/patient-service/register/depts`|
|查询科室医生|`GET`|`/patient-service/register/doctors/{deptId}`|
|查询医生详情|`GET`|`/patient-service/register/doctor/{doctorId}`|
|查询医生排班|`GET`|`/patient-service/register/schedules/{doctorId}`|
|提交挂号|`POST`|`/patient-service/register/submit`|
|查询患者挂号历史|`GET`|`/patient-service/register/history/{patientId}`|
|查询挂号详情|`GET`|`/patient-service/register/detail/{registerId}`|
|按挂号ID查询病历|`GET`|`/api/patient/medical/list`|
|按患者ID查询全部病历|`GET`|`/api/patient/medical/my-list`|
|按挂号ID查询处方|`GET`|`/api/patient/prescription/list`|
|按患者ID查询全部处方|`GET`|`/api/patient/prescription/my-list`|



#### 模块数据模型



##### `Patient`



|字段|类型|说明|
|---|---|---|
|patientId|String|患者ID|
|name|String|姓名|
|gender|Integer|1男，2女|
|phone|String|脱敏手机号|
|avatar|String|头像地址|
|idCard|String|脱敏身份证号|
|address|String|地址|
|birthday|Date|出生日期|
|createTime|DateTime|创建时间|
|updateTime|DateTime|更新时间|
|password|null|固定不返回密码|



##### `Dept`



|字段|类型|说明|
|---|---|---|
|deptId|String|科室ID|
|deptName|String|科室名称|
|roomId|String|房间ID|
|maxCapacity|Integer|最大容量|
|freeCapacity|Integer|可用容量|
|status|Integer|1启用|
|createTime|DateTime|创建时间|



##### `Doctor`



|字段|类型|说明|
|---|---|---|
|doctorId|String|医生ID|
|avatar|String|头像地址|
|name|String|姓名|
|gender|Integer|性别|
|phone|String|手机号，当前实体可能直接返回|
|email|String|邮箱|
|position|String|职称/职位|
|goodAt|String|擅长|
|introduction|String|简介|
|departmentId|String|科室ID|
|status|Integer|账号状态|



##### `DoctorDetailVo`



|字段|类型|说明|
|---|---|---|
|doctorId|String|医生ID|
|name|String|姓名|
|position|String|职称|
|goodAt|String|擅长|
|introduction|String|简介|
|avatar|String|头像|
|deptName|String|科室名称|
|schedules|ScheduleVo\[\]|排班列表|



##### `DoctorSchedule`



|字段|类型|说明|
|---|---|---|
|scheduleId|String|排班ID|
|doctorId|String|医生ID|
|doctorName|String|医生姓名|
|timeJson|String|时段JSON|
|maxNum|Integer|最大号源|
|remainNum|Integer|剩余号源|
|status|Integer|排班状态|
|price|Decimal|挂号费|
|room|String|诊室|
|createTime|DateTime|创建时间|



##### `Registration`



|字段|类型|说明|
|---|---|---|
|registerId|String|挂号ID|
|patientId|String|患者ID|
|doctorId|String|医生ID|
|name|String|患者姓名|
|gender|Integer|患者性别|
|birthday|Date|出生日期|
|chiefComplaint|String|主诉|
|department|String|当前代码写入医生position|
|consultRoom|String|诊室|
|visitDate|Date|就诊日期|
|consultTime|String|就诊时段|
|price|Decimal|费用|
|payStatus|String|初始WAITING|
|createTime|DateTime|创建时间|



##### `MedicalRecord`



|字段|类型|说明|
|---|---|---|
|recordId|String|病历ID|
|patientId|String|患者ID|
|doctorId|String|医生ID|
|registerId|String|挂号ID|
|doctorName|String|医生姓名|
|patientName|String|患者姓名|
|visitAge|Integer|就诊年龄|
|description|String|病历内容|
|visitDate|Date|就诊日期|
|payStatus|String|支付状态|
|createTime|DateTime|创建时间|



##### `Prescription`



|字段|类型|说明|
|---|---|---|
|prescriptionId|String|处方ID|
|registerId|String|挂号ID|
|patientId|String|患者ID|
|doctorId|String|医生ID|
|medicineName|String|药品名称|
|dosage|String|剂量|
|usage\_|String|用法|
|quantity|Integer|数量|
|price|Decimal|价格|
|payStatus|String|支付状态|
|createTime|DateTime|创建时间|



#### 2\.2\.1 患者个人中心



##### 2\.2\.1\.1 查询患者个人信息



|项目|内容|
|---|---|
|接口地址|`/api/patient/profile/info`|
|请求方式|`GET`|
|请求头|token: JWT（必填）|
|权限说明|患者本人|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Patient|脱敏患者信息（见数据模型）|



**请求示例**



```HTTP
GET /api/patient/profile/info
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "patientId": "p1718000000000ABCD",
    "name": "张三",
    "gender": 1,
    "phone": "138****0000",
    "idCard": "3301**********1234",
    "address": "杭州市",
    "birthday": "1991-05-12",
    "password": null
  }
}
```



**错误码：** 500 未登录/Token无效；500 患者不存在



**业务规则：** patientId从JWT解析，不允许直接传patientId代替token；手机号、身份证脱敏；password置空。



##### 2\.2\.1\.2 更新患者基础信息



|项目|内容|
|---|---|
|接口地址|`/api/patient/profile/update`|
|请求方式|`PUT`|
|请求头|token: JWT（必填）|
|权限说明|患者本人|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|name|body|String|否|姓名|
|gender|body|String|否|男/女或1/2|
|address|body|String|否|地址|
|birthday|body|Date|否|yyyy\-MM\-dd|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|null|无业务数据|



**请求示例**



```JSON
{
  "name": "张三",
  "gender": "1",
  "address": "杭州市",
  "birthday": "1991-05-12"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```



**错误码：** 500 Token无效；500 患者不存在；500 性别参数错误



**业务规则：** 只更新非空字段；birthday必须可解析为ISO日期。



##### 2\.2\.1\.3 上传患者头像



|项目|内容|
|---|---|
|接口地址|`/api/patient/profile/avatar-upload`|
|请求方式|`POST`|
|请求头|token \+ multipart/form\-data|
|权限说明|患者本人|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|file|multipart|File|是|图片文件，仅支持 jpg/jpeg/png/gif/webp，最大 2MB|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|avatarUrl|String|生成的访问路径|



**请求示例**



```Plain Text
multipart/form-data: file=<binary>
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "avatarUrl": "/files/avatar/patient/p1718_1718000000000.png"
  }
}
```



**错误码：** 500 Token无效；500 患者不存在；500 头像文件为空/超过2MB/格式不支持



**业务规则：** 当前实现仅生成路径，未保存文件，也未更新患者头像字段；上线前需补齐存储逻辑。



##### 2\.2\.1\.4 修改患者手机号



|项目|内容|
|---|---|
|接口地址|`/api/patient/profile/change-phone`|
|请求方式|`POST`|
|请求头|token: JWT（必填）|
|权限说明|患者本人|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|oldPhone|body|String|是|原手机号|
|newPhone|body|String|是|新手机号|
|smsCode|body|String|是|当前实现接收但未校验|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|null|无业务数据|



**请求示例**



```JSON
{
  "oldPhone": "13800000000",
  "newPhone": "13900000000",
  "smsCode": "123456"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```



**错误码：** 500 原手机号不正确；500 新手机号已被使用



**业务规则：** 校验原手机号；新手机号不可被占用；当前未校验短信验证码。



##### 2\.2\.1\.5 修改患者密码



|项目|内容|
|---|---|
|接口地址|`/api/patient/profile/change-password`|
|请求方式|`POST`|
|请求头|token: JWT（必填）|
|权限说明|患者本人|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|oldPassword|body|String|是|原密码|
|newPassword|body|String|是|新密码|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|null|无业务数据|



**请求示例**



```JSON
{
  "oldPassword": "123456",
  "newPassword": "NewPass123"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```



**错误码：** 500 原密码不正确；500 患者不存在



**业务规则：** 原密码与数据库MD5值比对，新密码以MD5保存；当前未做复杂度校验。



##### 2\.2\.1\.6 身份证敏感操作二次验证



|项目|内容|
|---|---|
|接口地址|`/api/patient/profile/verify-idcard`|
|请求方式|`POST`|
|请求头|token: JWT（必填）|
|权限说明|患者本人|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|password|body|String|是|当前登录密码|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|null|验证通过无数据|



**请求示例**



```JSON
{
  "password": "123456"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```



**错误码：** 500 密码验证失败；500 患者不存在



**业务规则：** 仅验证密码，不修改身份证信息。



##### 2\.2\.1\.7 查询患者挂号记录



|项目|内容|
|---|---|
|接口地址|`/api/patient/profile/registers`|
|请求方式|`GET`|
|请求头|token: JWT（必填）|
|权限说明|患者本人|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Registration\[\]|挂号记录列表|



**请求示例**



```HTTP
GET /api/patient/profile/registers
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "registerId": "REG001",
      "patientId": "p1718000000000ABCD",
      "doctorId": "DOC001",
      "name": "张三",
      "chiefComplaint": "咳嗽三天",
      "department": "主任医师",
      "consultRoom": "101",
      "visitDate": "2026-06-16",
      "consultTime": "09:00-09:30",
      "price": 30,
      "payStatus": "WAITING"
    }
  ]
}
```



**错误码：** 500 Token无效



**业务规则：** patientId从token解析。



##### 2\.2\.1\.8 查询患者缴费记录



|项目|内容|
|---|---|
|接口地址|`/api/patient/profile/payments`|
|请求方式|`GET`|
|请求头|token: JWT（必填）|
|权限说明|患者本人|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Object|payment\-service Feign原样结果|



**请求示例**



```HTTP
GET /api/patient/profile/payments
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "list": [
      {
        "orderId": "PAY001",
        "patientId": "p1718000000000ABCD",
        "registerId": "REG001",
        "payType": "REGISTER",
        "description": "挂号费",
        "amount": 30,
        "payStatus": "PAID",
        "payMethod": "WECHAT",
        "payTime": "2026-06-14T09:30:00"
      }
    ],
    "total": 1
  }
}
```



**错误码：** 500 Token无效



**业务规则：** payment\-service不可用时Feign降级返回空list和total=0。



##### 2\.2\.1\.9 查询个人中心聚合数据



|项目|内容|
|---|---|
|接口地址|`/api/patient/profile/full`|
|请求方式|`GET`|
|请求头|token: JWT（必填）|
|权限说明|患者本人|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|profile|Patient|脱敏资料|
|registers|Registration\[\]|挂号记录|
|payments|Object|缴费服务结果|



**请求示例**



```HTTP
GET /api/patient/profile/full
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "profile": {
      "patientId": "p1718000000000ABCD",
      "name": "张三",
      "gender": 1,
      "phone": "138****0000",
      "idCard": "3301**********1234",
      "address": "杭州市",
      "birthday": "1991-05-12",
      "password": null
    },
    "registers": [
      {
        "registerId": "REG001",
        "patientId": "p1718000000000ABCD",
        "doctorId": "DOC001",
        "name": "张三",
        "chiefComplaint": "咳嗽三天",
        "department": "主任医师",
        "consultRoom": "101",
        "visitDate": "2026-06-16",
        "consultTime": "09:00-09:30",
        "price": 30,
        "payStatus": "WAITING"
      }
    ],
    "payments": {
      "list": [
        {
          "orderId": "PAY001",
          "patientId": "p1718000000000ABCD",
          "registerId": "REG001",
          "payType": "REGISTER",
          "description": "挂号费",
          "amount": 30,
          "payStatus": "PAID",
          "payMethod": "WECHAT",
          "payTime": "2026-06-14T09:30:00"
        }
      ],
      "total": 1
    }
  }
}
```



**错误码：** 500 Token无效；500 患者不存在



**业务规则：** 一次聚合资料、挂号和缴费；payment\-service故障时缴费部分降级为空。



#### 2\.2\.2 患者内部服务



##### 2\.2\.2\.1 按患者ID查询资料



|项目|内容|
|---|---|
|接口地址|`/api/patient/info/{patientId}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|仅服务间调用；当前网关路径可达，需补充隔离|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|patientId|path|String|是|患者ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Patient|脱敏患者信息|



**请求示例**



```HTTP
GET /api/patient/info/{patientId}
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "patientId": "p1718000000000ABCD",
    "name": "张三",
    "gender": 1,
    "phone": "138****0000",
    "idCard": "3301**********1234",
    "address": "杭州市",
    "birthday": "1991-05-12",
    "password": null
  }
}
```



**错误码：** 500 患者不存在



**业务规则：** 供AI/挂号等服务调用；返回脱敏信息。



##### 2\.2\.2\.2 按患者ID查询挂号历史



|项目|内容|
|---|---|
|接口地址|`/api/patient/register/history/{patientId}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|仅服务间调用；当前网关路径可达，需补充隔离|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|patientId|path|String|是|患者ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Registration\[\]|挂号记录|



**请求示例**



```HTTP
GET /api/patient/register/history/{patientId}
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "registerId": "REG001",
      "patientId": "p1718000000000ABCD",
      "doctorId": "DOC001",
      "name": "张三",
      "chiefComplaint": "咳嗽三天",
      "department": "主任医师",
      "consultRoom": "101",
      "visitDate": "2026-06-16",
      "consultTime": "09:00-09:30",
      "price": 30,
      "payStatus": "WAITING"
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



#### 2\.2\.3 患者挂号



##### 2\.2\.3\.1 获取启用科室



|项目|内容|
|---|---|
|接口地址|`/patient-service/register/depts`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|患者端/开放查询|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Dept\[\]|启用科室列表|



**请求示例**



```HTTP
GET /patient-service/register/depts
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "deptId": "D001",
      "deptName": "内科",
      "roomId": "R101",
      "maxCapacity": 100,
      "freeCapacity": 42,
      "status": 1,
      "createTime": "2026-05-01T08:00:00"
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 仅查询status=1的科室，按createTime升序。



##### 2\.2\.3\.2 查询科室医生



|项目|内容|
|---|---|
|接口地址|`/patient-service/register/doctors/{deptId}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|患者端/开放查询|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|deptId|path|String|是|科室ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Doctor\[\]|医生列表|



**请求示例**



```HTTP
GET /patient-service/register/doctors/{deptId}
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "doctorId": "DOC001",
      "name": "李医生",
      "position": "主任医师",
      "goodAt": "呼吸系统疾病",
      "departmentId": "D001",
      "status": 1
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.2\.3\.3 查询医生详情



|项目|内容|
|---|---|
|接口地址|`/patient-service/register/doctor/{doctorId}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|患者端/开放查询|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|doctorId|path|String|是|医生ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|DoctorDetailVo|医生详情及排班|



**请求示例**



```HTTP
GET /patient-service/register/doctor/{doctorId}
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "doctorId": "DOC001",
    "name": "李医生",
    "position": "主任医师",
    "goodAt": "呼吸系统疾病",
    "introduction": "从业15年",
    "avatar": "/files/avatar/doc001.png",
    "deptName": "内科",
    "schedules": [
      {
        "scheduleId": "SCH001",
        "timeJson": "[\"09:00-09:30\"]",
        "maxNum": 20,
        "remainNum": 8,
        "price": 30,
        "room": "101"
      }
    ]
  }
}
```



**错误码：** 500 医生不存在



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.2\.3\.4 查询医生排班



|项目|内容|
|---|---|
|接口地址|`/patient-service/register/schedules/{doctorId}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|患者端/开放查询|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|doctorId|path|String|是|医生ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|DoctorSchedule\[\]|排班列表|



**请求示例**



```HTTP
GET /patient-service/register/schedules/{doctorId}
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "scheduleId": "SCH001",
      "doctorId": "DOC001",
      "doctorName": "李医生",
      "timeJson": "[\"09:00-09:30\"]",
      "maxNum": 20,
      "remainNum": 8,
      "status": 1,
      "price": 30,
      "room": "101"
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.2\.3\.5 提交挂号



|项目|内容|
|---|---|
|接口地址|`/patient-service/register/submit`|
|请求方式|`POST`|
|请求头|JSON|
|权限说明|患者端|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|patientId|body|String|是|患者ID|
|doctorId|body|String|是|医生ID|
|scheduleId|body|String|是|排班ID|
|chiefComplaint|body|String|否|主诉|
|visitDate|body|Date|是|yyyy\-MM\-dd|
|consultTime|body|String|是|就诊时段|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Registration|新建挂号记录|



**请求示例**



```JSON
{
  "patientId": "p1718000000000ABCD",
  "doctorId": "DOC001",
  "scheduleId": "SCH001",
  "chiefComplaint": "咳嗽三天",
  "visitDate": "2026-06-16",
  "consultTime": "09:00-09:30"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "registerId": "REG001",
    "patientId": "p1718000000000ABCD",
    "doctorId": "DOC001",
    "name": "张三",
    "chiefComplaint": "咳嗽三天",
    "department": "主任医师",
    "consultRoom": "101",
    "visitDate": "2026-06-16",
    "consultTime": "09:00-09:30",
    "price": 30,
    "payStatus": "WAITING"
  }
}
```



**错误码：** 500 患者/医生/排班不存在；500 号源已满；500 扣减号源失败



**业务规则：** 事务内校验患者、医生、排班和剩余号源；提交时立即扣减号源；payStatus初始WAITING。



##### 2\.2.3.5 查询患者挂号历史



|项目|内容|
|---|---|
|接口地址|`/patient-service/register/history/{patientId}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|患者本人或内部服务|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|patientId|path|String|是|患者ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Registration\[\]|挂号记录|



**请求示例**



```HTTP
GET /patient-service/register/history/{patientId}
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "registerId": "REG001",
      "patientId": "p1718000000000ABCD",
      "doctorId": "DOC001",
      "name": "张三",
      "chiefComplaint": "咳嗽三天",
      "department": "主任医师",
      "consultRoom": "101",
      "visitDate": "2026-06-16",
      "consultTime": "09:00-09:30",
      "price": 30,
      "payStatus": "WAITING"
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.2\.3\.7 查询挂号详情



|项目|内容|
|---|---|
|接口地址|`/patient-service/register/detail/{registerId}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|患者本人/医生|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|path|String|是|挂号ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Registration|挂号详情|



**请求示例**



```HTTP
GET /patient-service/register/detail/{registerId}
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "registerId": "REG001",
    "patientId": "p1718000000000ABCD",
    "doctorId": "DOC001",
    "name": "张三",
    "chiefComplaint": "咳嗽三天",
    "department": "主任医师",
    "consultRoom": "101",
    "visitDate": "2026-06-16",
    "consultTime": "09:00-09:30",
    "price": 30,
    "payStatus": "WAITING"
  }
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



#### 2\.2\.4 患者就诊资料



##### 2\.2\.4\.1 按挂号ID查询病历



|项目|内容|
|---|---|
|接口地址|`/api/patient/medical/list`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|患者本人/医生|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|query|String|是|挂号ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|MedicalRecord\[\]|病历列表|



**请求示例**



```HTTP
GET /api/patient/medical/list
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "recordId": "REC001",
      "patientId": "p1718000000000ABCD",
      "doctorId": "DOC001",
      "registerId": "REG001",
      "doctorName": "李医生",
      "patientName": "张三",
      "visitAge": 35,
      "description": "咳嗽三天，无发热。",
      "visitDate": "2026-06-16",
      "payStatus": "PAID"
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.2\.4\.2 按患者ID查询全部病历



|项目|内容|
|---|---|
|接口地址|`/api/patient/medical/my-list`|
|请求方式|`GET`|
|请求头|token: 患者JWT（必填）|
|权限说明|患者本人|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|token|header|String|是|患者JWT；patientId 从 token 解析，忽略 query 中的 patientId|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|MedicalRecord\[\]|病历列表|



**请求示例**



```HTTP
GET /api/patient/medical/my-list
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "recordId": "REC001",
      "patientId": "p1718000000000ABCD",
      "doctorId": "DOC001",
      "registerId": "REG001",
      "doctorName": "李医生",
      "patientName": "张三",
      "visitAge": 35,
      "description": "咳嗽三天，无发热。",
      "visitDate": "2026-06-16",
      "payStatus": "PAID"
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** patientId 从 token 解析，不允许客户端通过 query 任意指定。



##### 2\.2\.4\.3 按挂号ID查询处方



|项目|内容|
|---|---|
|接口地址|`/api/patient/prescription/list`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|患者本人/医生|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|query|String|是|挂号ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Prescription\[\]|处方列表|



**请求示例**



```HTTP
GET /api/patient/prescription/list
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "prescriptionId": "PRE001",
      "registerId": "REG001",
      "patientId": "p1718000000000ABCD",
      "doctorId": "DOC001",
      "medicineName": "示例药品",
      "dosage": "10mg",
      "usage_": "每日两次",
      "quantity": 1,
      "price": 25.5,
      "payStatus": "WAITING"
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.2\.4\.4 按患者ID查询全部处方



|项目|内容|
|---|---|
|接口地址|`/api/patient/prescription/my-list`|
|请求方式|`GET`|
|请求头|token: 患者JWT（必填）|
|权限说明|患者本人|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|token|header|String|是|患者JWT；patientId 从 token 解析，忽略 query 中的 patientId|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Prescription\[\]|处方列表|



**请求示例**



```HTTP
GET /api/patient/prescription/my-list
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "prescriptionId": "PRE001",
      "registerId": "REG001",
      "patientId": "p1718000000000ABCD",
      "doctorId": "DOC001",
      "medicineName": "示例药品",
      "dosage": "10mg",
      "usage_": "每日两次",
      "quantity": 1,
      "price": 25.5,
      "payStatus": "WAITING"
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** patientId 从 token 解析，不允许客户端通过 query 任意指定。

### 2\.3 医生业务模块



**模块职责：** 负责医生资料维护、接诊工作台、病历确认、检查申请单、排班查询和检验申请查看。



**路由范围：** `/doctor-service/**`、`/inspection-doctor/**`



**权限说明：** 仅医生访问；涉及患者数据时必须校验就诊记录归属。



**接口数量：** 18



#### 模块接口清单



|接口名称|请求方式|接口地址|
|---|---|---|
|查询医生资料|`GET`|`/doctor-service/profile/info`|
|更新医生资料|`PUT`|`/doctor-service/profile/update`|
|查询资料完善状态|`GET`|`/doctor-service/profile/setup-status`|
|查询接诊列表|`GET`|`/doctor-service/consult/list`|
|查询接诊详情|`GET`|`/doctor-service/consult/detail`|
|暂存病历草稿|`POST`|`/doctor-service/consult/save-draft`|
|确认正式病历|`POST`|`/doctor-service/consult/confirm-record`|
|创建检查申请单|`POST`|`/doctor-service/consult/create-exam-order`|
|完成接诊|`POST`|`/doctor-service/consult/complete`|
|开具处方|`POST`|`/doctor-service/consult/create-prescription`|
|按挂号ID查询处方|`GET`|`/doctor-service/consult/prescription-list`|
|按挂号ID查询检查单|`GET`|`/doctor-service/exam-order/list`|
|查询当前医生开具的检查单|`GET`|`/doctor-service/exam-order/my-list`|
|查询医生周排班|`GET`|`/doctor-service/schedule/weekly`|
|查询医生可选药品列表|`GET`|`/doctor-service/medicine/list`|
|查询所有检验申请列表|`GET`<br>|`/inspection-doctor/lab-orders`|
|查询检验申请详情|`GET`<br>|`/inspection-doctor/order/{orderId}`|



#### 模块数据模型



##### `DoctorProfileVo`



|字段|类型|说明|
|---|---|---|
|name|String|姓名|
|deptName|String|科室|
|position|String|职称|
|avatar|String|头像|
|goodAt|String|擅长|
|introduction|String|简介|
|email|String|邮箱|



##### `ConsultRecord`



|字段|类型|说明|
|---|---|---|
|registerId|String|挂号ID|
|patientId|String|患者ID|
|doctorId|String|医生ID|
|name/patientName|String|患者姓名|
|gender|Integer|性别|
|birthday|Date|出生日期|
|chiefComplaint|String|主诉|
|department|String|科室|
|consultRoom|String|诊室|
|visitDate|Date|就诊日期|
|consultTime|String|时段|
|price|Decimal|费用|
|payStatus|String|支付状态|
|consultStatus|String|接诊状态|
|recordId|String|病历ID，可为空|
|description|String|病历内容|
|patientAge|Integer|计算后的年龄|
|createTime|DateTime|创建时间|



##### `ExamOrder`



|字段|类型|说明|
|---|---|---|
|reportId|String|检查单ID|
|patientId|String|患者ID|
|registerId|String|挂号ID|
|doctorId|String|医生ID|
|patientName|String|患者姓名|
|gender|Integer|性别|
|age|Integer|年龄|
|checkType|String|检查类型|
|checkItem|String|检查项目|
|price|Decimal|费用|
|payStatus|String|支付状态|
|urgencyLevel|String|紧急程度|
|createTime|DateTime|创建时间|



##### `Schedule`



|字段|类型|说明|
|---|---|---|
|scheduleId|String|排班ID|
|doctorId|String|医生ID|
|deptId|String|科室ID|
|workDate|Date|工作日期|
|timeSlot|String|时段|
|maxNum|Integer|最大号源|
|remainNum|Integer|剩余号源|
|price|Decimal|费用|
|status|String|排班状态|
|createTime|DateTime|创建时间|



##### `MedicineOption`



|字段|类型|说明|
|---|---|---|
|medicineId|String|药品ID|
|name|String|药品名称|
|spec|String|规格|
|usage|String|默认用法用量|
|indication|String|适应症|
|attention|String|注意事项|
|stock|Integer|库存|
|price|Decimal|单价|
|createTime|DateTime|创建时间|



#### 2\.3\.1 医生个人资料



##### 2\.3\.1\.1 查询医生资料



|项目|内容|
|---|---|
|接口地址|`/doctor-service/profile/info`|
|请求方式|`GET`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|医生本人|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|DoctorProfileVo|医生资料|



**请求示例**



```HTTP
GET /doctor-service/profile/info
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "name": "李医生",
    "deptName": "内科",
    "position": "主任医师",
    "avatar": "/files/avatar/doc001.png",
    "goodAt": "呼吸系统疾病",
    "introduction": "从业15年",
    "email": "doctor@example.com"
  }
}
```



**错误码：** 500 未登录/医生不存在



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.3\.1\.2 更新医生资料



|项目|内容|
|---|---|
|接口地址|`/doctor-service/profile/update`|
|请求方式|`PUT`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|医生本人|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|goodAt|body|String|否|擅长|
|introduction|body|String|否|简介|
|email|body|String|否|邮箱|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|null|无业务数据|



**请求示例**



```JSON
{
  "goodAt": "呼吸系统疾病",
  "introduction": "从业15年",
  "email": "doctor@example.com"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```



**错误码：** 500 医生不存在



**业务规则：** 只更新非null字段；姓名、科室和职称不可通过此接口修改。



##### 2\.3\.1\.3 查询资料完善状态



|项目|内容|
|---|---|
|接口地址|`/doctor-service/profile/setup-status`|
|请求方式|`GET`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|医生本人|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|needSetup|Boolean|擅长或简介为空时为true|



**请求示例**



```HTTP
GET /doctor-service/profile/setup-status
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "needSetup": false
  }
}
```



**错误码：** 500 医生不存在



**业务规则：** 仅依据goodAt和introduction判断。



#### 2\.3\.2 医生接诊



##### 2\.3\.2\.1 查询接诊列表



|项目|内容|
|---|---|
|接口地址|`/doctor-service/consult/list`|
|请求方式|`GET`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|医生本人|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|consultStatus|query|String|否|接诊状态|
|date|query|String|否|日期过滤|
|page|query|Integer|否|默认1|
|limit|query|Integer|否|默认10|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|ConsultRecord\[\]|接诊列表|



**请求示例**



```HTTP
GET /doctor-service/consult/list
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "registerId": "REG001",
      "patientId": "p1718000000000ABCD",
      "doctorId": "DOC001",
      "name": "张三",
      "chiefComplaint": "咳嗽三天",
      "consultStatus": "IN_PROGRESS",
      "description": "初步病历",
      "patientAge": 35
    }
  ]
}
```



**错误码：** 500 Token无效



**业务规则：** doctorId从token解析；offset=\(page\-1\)\*limit；当前未限制page/limit下界。



##### 2\.3\.2\.2 查询接诊详情



|项目|内容|
|---|---|
|接口地址|`/doctor-service/consult/detail`|
|请求方式|`GET`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|医生；当前实现未校验记录归属|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|query|String|是|挂号ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|ConsultRecord|接诊详情|



**请求示例**



```HTTP
GET /doctor-service/consult/detail
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "registerId": "REG001",
    "patientId": "p1718000000000ABCD",
    "doctorId": "DOC001",
    "name": "张三",
    "chiefComplaint": "咳嗽三天",
    "consultStatus": "IN_PROGRESS",
    "description": "初步病历",
    "patientAge": 35
  }
}
```



**错误码：** 500 就诊记录不存在



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.3\.2\.3 暂存病历草稿



|项目|内容|
|---|---|
|接口地址|`/doctor-service/consult/save-draft`|
|请求方式|`POST`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|医生；当前实现未校验记录归属|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|挂号ID|
|recordDesc|body|String|是|病历内容|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|null|无业务数据|



**请求示例**



```JSON
{
  "registerId": "REG001",
  "recordDesc": "主诉：咳嗽三天。"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```



**错误码：** 500 就诊记录不存在；500 病历内容为空



**业务规则：** 新增或更新病历，并将接诊状态标记为IN\_PROGRESS。



##### 2\.3\.2\.4 确认正式病历



|项目|内容|
|---|---|
|接口地址|`/doctor-service/consult/confirm-record`|
|请求方式|`POST`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|记录所属医生|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|挂号ID|
|recordDesc|body|String|是|正式病历内容|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|null|无业务数据|



**请求示例**



```JSON
{
  "registerId": "REG001",
  "recordDesc": "正式病历内容"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```



**错误码：** 400 字段为空；500 无权操作；500 就诊记录不存在



**业务规则：** 校验当前医生拥有该就诊记录；新增或更新病历；状态改为RECORD\_CONFIRMED。



##### 2\.3\.2\.5 创建检查申请单



|项目|内容|
|---|---|
|接口地址|`/doctor-service/consult/create-exam-order`|
|请求方式|`POST`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|医生；当前实现未校验记录归属|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|挂号ID|
|checkItemList|body|String|是|检查项目文本|
|urgencyLevel|body|String|否|当前实现接收但未写入|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|null|无业务数据|



**请求示例**



```JSON
{
  "registerId": "REG001",
  "checkItemList": "胸部CT,血常规",
  "urgencyLevel": "NORMAL"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```



**错误码：** 500 就诊记录不存在



**业务规则：** 生成CHK前缀检查单；checkType固定为“检查”，price固定0；urgencyLevel当前未持久化。



##### 2\.3\.2\.6 完成接诊



|项目|内容|
|---|---|
|接口地址|`/doctor-service/consult/complete`|
|请求方式|`POST`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|医生；当前实现未校验记录归属|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|挂号ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|null|无业务数据|



**请求示例**



```JSON
{
  "registerId": "REG001"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 直接更新接诊状态；当前未检查病历是否已确认。



##### 2\.3\.2\.7 开具处方



|项目|内容|
|---|---|
|接口地址|`/doctor-service/consult/create-prescription`|
|请求方式|`POST`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|记录所属医生；已完成接诊不能继续开具处方|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|挂号ID|
|medicineId|body|String|否|药品ID；当前前端处方审查会选择真实药品并传入该字段|
|medicineName|body|String|是|药品名称|
|spec|body|String|否|规格|
|usage|body|String|否|用法用量|
|num|body|Integer|是|数量|
|price|body|Decimal|否|单价|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Prescription|已创建的处方记录|



**请求示例**



```JSON
{
  "registerId": "REG001",
  "medicineId": "MED001",
  "medicineName": "示例药品",
  "spec": "10mg×20片",
  "usage": "口服，每次1片，每日2次",
  "num": 1,
  "price": 25.5
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "prescriptionId": "PRE0123456789ABCDEF01234567",
    "registerId": "REG001",
    "doctorId": "DOC001",
    "medicineId": "MED001",
    "medicineName": "示例药品",
    "spec": "10mg×20片",
    "usage": "口服，每次1片，每日2次",
    "num": 1,
    "price": 25.5,
    "payStatus": "WAITING",
    "createTime": "2026-06-16T09:30:00"
  }
}
```



**错误码：** 500 Token无效；500 就诊记录不存在；500 接诊已完成不能继续开具处方。



**业务规则：** 当前实现创建处方并将 `payStatus` 固定为 `WAITING`，返回体会回填 `payStatus` 和 `createTime`；`medicineId` 为兼容新增字段，传入时写入 `prescription.medicine_id`，旧调用不传仍可创建处方；开方本身不扣减药品库存。



##### 2\.3\.2\.8 按挂号ID查询处方



|项目|内容|
|---|---|
|接口地址|`/doctor-service/consult/prescription-list`|
|请求方式|`GET`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|记录所属医生|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|query|String|是|挂号ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|Prescription\[\]|处方列表|



**请求示例**



```HTTP
GET /doctor-service/consult/prescription-list?registerId=REG001
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "prescriptionId": "PRE0123456789ABCDEF01234567",
      "registerId": "REG001",
      "doctorId": "DOC001",
      "medicineId": "MED001",
      "medicineName": "示例药品",
      "spec": "10mg×20片",
      "usage": "口服，每次1片，每日2次",
      "num": 1,
      "price": 25.5,
      "payStatus": "WAITING"
    }
  ]
}
```



**错误码：** 500 Token无效；500 就诊记录不存在或无权访问。



**业务规则：** 查询前复用接诊详情归属校验；按创建时间倒序返回。



#### 2\.3\.3 医生检查单



##### 2\.3\.3\.1 按挂号ID查询检查单



|项目|内容|
|---|---|
|接口地址|`/doctor-service/exam-order/list`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|医生|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|query|String|是|挂号ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|ExamOrder\[\]|检查单列表|



**请求示例**



```HTTP
GET /doctor-service/exam-order/list?registerId=REG001&doctorId=DOC001
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "reportId": "CHK0123456789ABCDEF",
      "patientId": "p1718000000000ABCD",
      "registerId": "REG001",
      "doctorId": "DOC001",
      "patientName": "张三",
      "checkType": "检查",
      "checkItem": "胸部CT",
      "price": 0,
      "urgencyLevel": "NORMAL"
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.3\.3\.2 查询当前医生开具的检查单



|项目|内容|
|---|---|
|接口地址|`/doctor-service/exam-order/my-list`|
|请求方式|`GET`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|医生本人|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|ExamOrder\[\]|检查单列表|



**请求示例**



```HTTP
GET /doctor-service/exam-order/my-list
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "reportId": "CHK0123456789ABCDEF",
      "patientId": "p1718000000000ABCD",
      "registerId": "REG001",
      "doctorId": "DOC001",
      "patientName": "张三",
      "checkType": "检查",
      "checkItem": "胸部CT",
      "price": 0,
      "urgencyLevel": "NORMAL"
    }
  ]
}
```



**错误码：** 500 Token无效



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



#### 2\.3\.4 医生排班



##### 2\.3\.4\.1 查询医生周排班



|项目|内容|
|---|---|
|接口地址|`/doctor-service/schedule/weekly`|
|请求方式|`GET`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|医生本人|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|WeeklyScheduleVo|周排班视图|



**请求示例**



```HTTP
GET /doctor-service/schedule/weekly?weekStart=2026-06-15
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "weekStart": "2026-06-15",
    "weekEnd": "2026-06-21",
    "weekData": [
      {
        "date": "2026-06-16",
        "dayOfWeek": 2,
        "dayName": "周二",
        "schedules": [
          {
            "scheduleId": "SCH001",
            "doctorId": "DOC001",
            "deptId": "D001",
            "workDate": "2026-06-16",
            "timeSlot": "09:00-12:00",
            "maxNum": 20,
            "remainNum": 8,
            "price": 30,
            "status": "ACTIVE"
          }
        ]
      }
    ]
  }
}
```



**错误码：** 500 Token无效



**业务规则：** `weekStart` 为空时返回当前周；返回一周七天数据，没有排班的日期 `schedules` 为空数组。

#### 2\.3\.5 医生可选药品



##### 2\.3\.5\.1 查询医生可选药品列表



|项目|内容|
|---|---|
|接口地址|`/doctor-service/medicine/list`|
|请求方式|`GET`|
|请求头|token: 医生JWT（必填；当前代码兼容直接传doctorId，仅限开发）|
|权限说明|医生本人；只读查询，不提供新增、更新、扣库存能力|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|keyword|query|String|否|按药品名称模糊查询|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|MedicineOption\[\]|药品列表|



**请求示例**



```HTTP
GET /doctor-service/medicine/list?keyword=阿莫西林
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "medicineId": "MED001",
      "name": "阿莫西林胶囊",
      "spec": "0.25g×24粒",
      "usage": "口服，每次0.5g，每日3次",
      "indication": "细菌感染",
      "attention": "青霉素过敏禁用",
      "stock": 120,
      "price": 18.5,
      "createTime": "2026-06-16T09:00:00"
    }
  ]
}
```



**错误码：** 500 未登录/Token无效。



**业务规则：** 医生开方和 AI 处方审查通过该接口选择真实 `medicineId`；接口仅读取 `medicine` 表，不扣减库存。

#### **2\.3\.6 检验申请查看**



##### **2.3.6\.1 查询所有检验申请列表**



|**项目**|**内容**|
|---|---|
|接口地址|`/inspection-doctor/lab-orders`|
|请求方式|`GET`|
|认证|需要医生登录 token|
|权限|检验医生角色|

**请求示例：**

```Plain Text
GET /inspection-doctor/lab-orders
```

**返回示例：**

```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "orderId": "MO202606160001",
      "patientId": "P202606140001",
      "patientName": "张明",
      "gender": 1,
      "age": 35,
      "registerId": "REG202606160001",
      "doctorId": "D202606140001",
      "clinicalSummary": "反复头痛伴眩晕3天，需排除颅内占位及脑血管异常。",
      "itemName": "脑脊液常规",
      "itemCode": "CSF_ROUTINE",
      "itemCategory": "LAB",
      "urgencyLevel": "URGENT",
      "sourceType": "AI_ASSISTED",
      "status": "QUEUED",
      "payStatus": "PAID",
      "confirmedTime": "2026-06-16T09:50:00+08:00",
      "createTime": "2026-06-16T09:48:00+08:00"
    }
  ]
}
```

##### **2.3.6\.2 查询检验申请详情**

|**项目**|**内容**|
|---|---|
|接口地址|`/inspection-doctor/order/{orderId}`|
|请求方式|`GET`|
|认证|需要医生登录 token|
|路径参数|orderId \- 医技申请ID|

**请求示例：**

```Plain Text
GET /inspection-doctor/order/MO202606160001
```

**返回示例：**

```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "orderId": "MO202606160001",
    "patientId": "P202606140001",
    "registerId": "REG202606160001",
    "doctorId": "D202606140001",
    "clinicalSummary": "反复头痛伴眩晕3天。",
    "urgencyLevel": "URGENT",
    "sourceType": "AI_ASSISTED",
    "aiTraceId": "AI9f2c7a11e8234d5c",
    "status": "QUEUED",
    "payStatus": "PAID",
    "confirmedTime": "2026-06-16T09:50:00+08:00",
    "createTime": "2026-06-16T09:48:00+08:00",
    "updateTime": "2026-06-16T10:00:00+08:00"
  }
}
```



### 2\.4 AI 能力模块



**模块职责：** 提供医生辅助接诊分析、AI 采纳反馈、药品问答流式输出、AI智能问诊（给患者推荐科室和医生）、AI报告分析，以及模块3负责的AI生成检查/检验项目建议。



**路由范围：** `/ai-service/**`、`/api/ai/**`



**权限说明：** 辅助接诊和报告分析接口仅医生使用；AI生成检查/检验项目建议由模块3维护；药品问答当前 Controller 尚未校验 token。



**接口数量：** 5



#### 模块接口清单



|接口名称|请求方式|接口地址|
|---|---|---|
|AI辅助接诊聊天|`POST`|`/ai-service/reception/chat`|
|AI生成检查/检验项目建议|`POST`|`/ai-service/agent/exam/generate`|
|AI报告分析|`POST`|`/ai-service/report/analyze`|
|药品问答流式输出|`POST`|`/api/ai/medicine/chat`|
|AI智能问诊<br>（给患者推荐科室和医生）|`POST`<br>|`/api/ai/consult/recommend`|



#### 模块数据模型



##### `AiAssistantChatRequest`



|字段|类型|说明|
|---|---|---|
|registerId|String|挂号ID，必填，最多32字符|
|message|String|医生自然语言问题，最多2000字符；与actionType至少提供一项|
|actionType|String|快捷功能意图，最多64字符；与message至少提供一项|
|currentRecordDesc|String|当前病历草稿，最多10000字符|
|symptomDescription|String|补充症状描述，最多5000字符|
|conversationText|String|医患对话或本次问题上下文，最多20000字符|
|structuredParameters|Map\<String,String\>|结构化问诊参数，最多30项|
|followUpAnswers|Map\<String,String\>|AI追问和患者回答，最多10项|
|patientInformation|Map\<String,String\>|处方审查补充患者信息，最多30项|
|medicines|PrescriptionReviewMedicineRequest\[\]|处方审查药品列表，最多10项|



##### `PrescriptionReviewMedicineRequest`



|字段|类型|说明|
|---|---|---|
|medicineId|String|药品ID，必填，最多32字符|
|usage|String|用法用量，必填，最多200字符|
|quantity|Integer|数量，必填，1到10000|



##### `AiAssistantChatResponse`



|字段|类型|说明|
|---|---|---|
|traceId|String|本次AI请求追踪ID|
|intent|String|识别出的意图，对应actionType枚举|
|answer|String|可直接展示在医生AI对话框中的文本回复|
|status|String|SUCCESS/DELEGATED/NEEDS_INPUT/FAILED/UNSUPPORTED|
|handledModule|String|实际处理模块；为空表示由辅助接诊模块直接处理|
|moduleResult|Object|专业模块结构化结果，例如病历草稿或处方审查结果|
|modelVersion|String|模型名称或版本|
|handledByAssistant|Boolean|是否由AI辅助接诊模块直接处理|
|fallback|Boolean|是否降级结果|



##### `ExamGenerateRequest`



|字段|类型|说明|
|---|---|---|
|registerId|String|挂号ID，必填|
|context|ExamGenerateContext|病历上下文，必填|



##### `ExamGenerateContext`



|字段|类型|说明|
|---|---|---|
|patientId|String|患者ID，必填|
|visitAge|Integer|就诊年龄|
|description|String|病历描述、主诉、现病史、体格检查、初步诊断等文本|



##### `ExamGenerateResponse`



|字段|类型|说明|
|---|---|---|
|traceId|String|本次生成追踪ID|
|clinicalSummary|String|临床摘要|
|checkItems|CheckItem\[\]|AI建议的检查/检验项目列表|
|urgencyLevel|String|紧急程度：NORMAL/URGENT/EMERGENCY|
|reasoningTrace|String|Agent执行追踪信息|



##### `CheckItem`



|字段|类型|说明|
|---|---|---|
|itemName|String|检查/检验项目名称|
|selected|Boolean|默认是否选中，供医生确认|



##### `ReportAnalysisDto`



|字段|类型|说明|
|---|---|---|
|registerId|String|挂号ID，必填|
|reportType|String|报告类型：EXAM/LAB|
|reportText|String|报告原文|
|indicators|IndicatorDto\[\]|检验指标明细，可选|



##### `IndicatorDto`



|字段|类型|说明|
|---|---|---|
|name|String|指标名称|
|value|String|指标值|
|unit|String|单位|
|referenceRange|String|参考范围|
|abnormalFlag|String|异常标记|



##### `ReportAnalysisVo`



|字段|类型|说明|
|---|---|---|
|summary|String|报告分析摘要|
|riskLevel|String|风险等级：LOW/MEDIUM/HIGH等|
|abnormalIndicators|AbnormalIndicatorVo\[\]|异常指标解释|
|suggestions|String\[\]|处理建议|
|followUpAdvice|String|随访或复查建议|
|fallback|Boolean|是否降级结果|



##### `AbnormalIndicatorVo`



|字段|类型|说明|
|---|---|---|
|name|String|指标名称|
|value|String|指标值|
|referenceRange|String|参考范围|
|interpretation|String|AI解释|



#### 2\.4\.1 AI辅助接诊



##### 2\.4\.1\.1 AI辅助接诊聊天



|项目|内容|
|---|---|
|接口地址|`/ai-service/reception/chat`|
|请求方式|`POST`|
|请求头|token: 医生JWT|
|权限说明|医生本人|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|挂号ID|
|message|body|String|否|医生自然语言问题；与actionType至少提供一项|
|actionType|body|String|否|快捷功能意图；与message至少提供一项|
|currentRecordDesc|body|String|否|当前病历，最多10000字符|
|symptomDescription|body|String|否|补充症状，最多5000字符|
|conversationText|body|String|否|医患对话或本次问题上下文，最多20000字符|
|structuredParameters|body|Map\<String,String\>|否|最多30项；AI病历生成至少应提供conversationText或非空structuredParameters|
|followUpAnswers|body|Map\<String,String\>|否|最多10项|
|patientInformation|body|Map\<String,String\>|否|处方审查补充患者信息，最多30项|
|medicines|body|PrescriptionReviewMedicineRequest\[\]|否|处方审查必填，最多10项|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|AiAssistantChatResponse|AI聊天响应|



**请求示例**



```JSON
{
  "registerId": "REG001",
  "message": "请基于当前接诊信息给出追问建议",
  "actionType": "FOLLOW_UP_QUESTION",
  "currentRecordDesc": "主诉：咳嗽三天。",
  "symptomDescription": "夜间加重",
  "followUpAnswers": {
    "是否发热？": "否"
  }
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "traceId": "AI0123456789abcdef",
    "intent": "FOLLOW_UP_QUESTION",
    "answer": "建议继续追问是否发热、咳痰颜色、胸痛气促和既往过敏史。",
    "status": "SUCCESS",
    "modelVersion": "deepseek-v4-flash",
    "handledByAssistant": true,
    "fallback": false
  }
}
```



**actionType枚举：** `FOLLOW_UP_QUESTION`、`MISSING_INFORMATION`、`CONTEXT_SUMMARY`、`CONTEXT_QA`、`MEDICAL_RECORD_DRAFT`、`PRESCRIPTION_REVIEW`、`DIAGNOSIS_ASSISTANT`。



**错误码：** 400 参数校验失败；200\+FAILED AI或依赖服务失败



**业务规则：** 通过内部接口校验医生与接诊记录归属；快捷按钮传入的actionType优先于关键词识别；`MEDICAL_RECORD_DRAFT`委派AI病历生成模块，`PRESCRIPTION_REVIEW`委派AI处方审查模块且必须提供真实medicineId、usage和quantity；AI失败时仍可能返回code=200，但data\.status=FAILED、fallback=true；AI结果仅供医生审核。



#### 2\.4\.2 AI生成检查/检验项目建议



##### 2\.4\.2\.1 AI生成检查/检验项目建议



|项目|内容|
|---|---|
|接口地址|`/ai-service/agent/exam/generate`|
|请求方式|`POST`|
|请求头|Content\-Type: application/json|
|权限说明|模块3维护；医生接诊模块仅跳转、传参、消费结果并由医生确认开单|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|挂号ID|
|context.patientId|body|String|是|患者ID|
|context.visitAge|body|Integer|否|就诊年龄|
|context.description|body|String|否|病历描述、主诉、现病史、体格检查、初步诊断等文本|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|ExamGenerateResponse|AI生成的检查/检验项目建议|



**请求示例**



```JSON
{
  "registerId": "REG001",
  "context": {
    "patientId": "P001",
    "visitAge": 35,
    "description": "主诉：头痛伴眩晕三天。现病史：..."
  }
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "traceId": "EXAM_0123456789abcdef",
    "clinicalSummary": "患者头痛伴眩晕，建议结合神经系统检查进一步评估。",
    "checkItems": [
      {
        "itemName": "颅脑CT平扫",
        "selected": true
      }
    ],
    "urgencyLevel": "NORMAL",
    "reasoningTrace": "=== Step 1: Planner ===\n..."
  }
}
```



**错误码：** 400 参数校验失败；200\+空checkItems AI生成失败或无建议。



**业务规则：** 该接口仅生成建议，不直接写入正式检查/检验申请；前端医生接诊页面可跳转到 `/doctor/ai-exam-generate` 或消费生成结果，但正式申请必须由医生确认后调用医生业务模块的开单接口。`/doctor/ai-exam-generate` 页面和该生成流程归模块3维护。



#### 2\.4\.3 AI报告分析



##### 2\.4\.3\.1 AI报告分析



|项目|内容|
|---|---|
|接口地址|`/ai-service/report/analyze`|
|请求方式|`POST`|
|请求头|token: 医生JWT|
|权限说明|医生本人；通过内部接诊上下文校验医生与挂号记录归属|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|挂号ID|
|reportType|body|String|否|报告类型：EXAM/LAB|
|reportText|body|String|否|报告原文|
|indicators|body|IndicatorDto\[\]|否|检验指标明细|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|ReportAnalysisVo|AI报告分析结果|



**请求示例**



```JSON
{
  "registerId": "REG001",
  "reportType": "LAB",
  "reportText": "脑脊液蛋白 0.62 g/L，参考范围 0.15-0.45 g/L"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "summary": "脑脊液蛋白偏高，需结合临床表现判断炎症或屏障受损可能。",
    "riskLevel": "MEDIUM",
    "abnormalIndicators": [
      {
        "name": "脑脊液蛋白",
        "value": "0.62 g/L",
        "referenceRange": "0.15-0.45 g/L",
        "interpretation": "高于参考范围"
      }
    ],
    "suggestions": ["核对报告原文和异常指标", "必要时结合影像和病史复查"],
    "followUpAdvice": "由医生结合查体和既往检查结果确认。",
    "fallback": false
  }
}
```



**错误码：** 500 Token无效；500 挂号ID为空；500 无法获取患者接诊信息。



**业务规则：** 调用前通过内部接口校验医生拥有该接诊记录；AI分析结果仅供医生参考，可由医生写入病历草稿。



#### 2\.4\.4 AI药品问答



##### 2\.4\.4\.1 药品问答流式输出



|项目|内容|
|---|---|
|接口地址|`/api/ai/medicine/chat`|
|请求方式|`POST`|
|请求头|Content\-Type: application/json；Accept: text/event\-stream|
|权限说明|医生端；当前Controller未校验token|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|sessionId|body|String|建议|会话ID|
|question|body|String|是|问题|
|medicineId|body|String|否|指定药品ID；为空时按问题关键词查找|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|SSE chunk|text|模型生成的文本片段|



**请求示例**



```JSON
{
  "sessionId": "S001",
  "question": "该药有哪些注意事项？",
  "medicineId": "MED001"
}
```



**返回示例**



```Plain Text
data: 药品使用需遵医嘱，注意禁忌证...

data: [后续文本片段]
```



**错误码：** 流中断/HTTP 5xx 模型、Redis或数据库异常



**业务规则：** 响应Content\-Type为text/event\-stream；超时300秒；会话历史当前不读取，仅写入Redis字符串，过期30分钟。

#### 2\.4\.5 AI智能问诊





### 2\.5 管理员 MLOps 模块



**模块职责：** 负责 AI 推理统计、日志审计、训练样本标注、模型训练和运行状态检查。



**路由范围：** `/api/admin/ml/**`



**权限说明：** 应仅管理员访问；当前 Controller 尚未执行管理员 token 校验。



**接口数量：** 9



#### 模块接口清单



|接口名称|请求方式|接口地址|
|---|---|---|
|推理统计|`GET`|`/api/admin/ml/dashboard/inference-stats`|
|模型统计|`GET`|`/api/admin/ml/dashboard/model-stats`|
|推理日志分页|`GET`|`/api/admin/ml/inference/logs`|
|训练样本分页|`GET`|`/api/admin/ml/samples/list`|
|更新训练样本标注|`POST`|`/api/admin/ml/samples/update`|
|模型列表|`GET`|`/api/admin/ml/models/list`|
|触发模型训练|`POST`|`/api/admin/ml/models/train`|
|训练任务列表|`GET`|`/api/admin/ml/models/tasks`|
|Python推理服务健康检查|`GET`|`/api/admin/ml/python/health`|
|CT伪影检测推理|`POST`|`/admin-service/ml/inference/ct-artifact`|
|CT伪影检测结果掩膜下载|`GET`|`/admin-service/ml/inference/ct-artifact/result/{maskFilename}`|

> **四角色拆分（CT 归属检查医生）**：CT 伪影检测虽物理上仍由 `ai-service` 的 `MlOpsController` 实现，但业务上归**检查医生（roleType=2, doctorType=2）**，不再属于管理员 MLOps。网关新增 doctor 向语义别名，前端检查医生页面应调用别名路径：
>
> | 检查医生调用路径（别名） | 网关重写后转发到 ai-service |
> |---|---|
> | `POST /doctor-service/exam/ct-artifact` | `POST /admin-service/ml/inference/ct-artifact` |
> | `GET /doctor-service/exam/ct-artifact/result/{maskFilename}` | `GET /admin-service/ml/inference/ct-artifact/result/{maskFilename}` |
>
> 路由 `doctor-ct-artifact-route` 置于通用 `/doctor-service/**` 路由之前（`gateway-server/application-route.yml`），通过 `RewritePath` 完成路径重写。原 `/admin-service/ml/inference/ct-artifact` 路径仍可用（向后兼容），但管理员端前端已移除 CT 入口。



#### 模块数据模型



##### `AiInferenceLog`



|字段|类型|说明|
|---|---|---|
|logId|String|日志ID|
|traceId|String|追踪ID|
|callSource|String|调用来源|
|modelKey|String|模型标识|
|modelVersion|String|模型版本|
|inputSummary|String|输入摘要|
|outputSummary|String|输出摘要|
|status|String|状态|
|durationMs|Integer|耗时毫秒|
|createdAt|DateTime|创建时间|
|patientId|String|患者ID，可为空|



##### `TrainingSample`



|字段|类型|说明|
|---|---|---|
|sampleId|String|样本ID|
|datasetName|String|数据集名称|
|filePath|String|文件路径|
|label|String|标注|
|labelType|String|标注类型|
|status|String|状态|
|createTime|DateTime|创建时间|



##### `ModelVersion`



|字段|类型|说明|
|---|---|---|
|modelId|String|模型ID|
|modelKey|String|模型标识|
|modelType|String|模型类型|
|version|String|版本|
|status|String|状态|
|createTime|DateTime|创建时间|

##### `CtArtifactInferenceResult`

|字段|类型|说明|
|---|---|---|
|status|String|Python 推理状态，成功为 success|
|message|String|推理结果提示|
|originalFile|String|原始上传文件名|
|maskFile|String|生成的掩膜文件名|
|shape|Integer\[\]|影像维度|
|spacing|Double\[\]|像素间距|
|origin|Double\[\]|影像原点|
|downloadUrl|String|掩膜下载路径；前端应通过 Java 代理下载|
|artifactDetected|Boolean|是否检测到金属伪影|
|positivePixels|Integer|伪影阳性像素/体素数|
|totalPixels|Integer|总像素/体素数|
|artifactRatio|Double|伪影占比，百分数|
|modelType|String|模型结构，当前默认 attention|
|modelVersion|String|模型版本，当前默认 attention_adamw_e4|
|summary|String|结构化摘要|
|reportInput|CtArtifactReportInput|给后续大语言模型生成文字描述/报告初稿的结构化输入|
|logId|String|Java 侧推理日志ID|
|latencyMs|Long|Java 调用 Python 总耗时毫秒|

##### `CtArtifactReportInput`

|字段|类型|说明|
|---|---|---|
|task|String|固定为 CT_ARTIFACT_REPORT|
|modality|String|固定为 CT|
|finding|Object|artifactDetected、positivePixels、totalPixels、artifactRatio、maskFile|
|imageMeta|Object|shape、spacing、origin|
|model|Object|modelType、modelVersion|
|summary|String|可直接交给大语言模型参考的摘要|



#### 2\.5\.1 管理员MLOps



##### 2\.5\.1\.1 推理统计



|项目|内容|
|---|---|
|接口地址|`/api/admin/ml/dashboard/inference-stats`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|管理员；当前Controller未校验token|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|todayTotal|Integer|今日调用数|
|successRate|Double|当前实现固定算法值|
|avgLatency|Long|平均耗时毫秒|



**请求示例**



```HTTP
GET /api/admin/ml/dashboard/inference-stats
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "todayTotal": 128,
    "successRate": 95.0,
    "avgLatency": 842
  }
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.5\.1\.2 模型统计



|项目|内容|
|---|---|
|接口地址|`/api/admin/ml/dashboard/model-stats`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|管理员；当前Controller未校验token|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|activeModels|Integer|当前实现固定为1|
|totalInference|Integer|总推理次数|



**请求示例**



```HTTP
GET /api/admin/ml/dashboard/model-stats
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "activeModels": 1,
    "totalInference": 10240
  }
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.5\.1\.3 推理日志分页



|项目|内容|
|---|---|
|接口地址|`/api/admin/ml/inference/logs`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|管理员；当前Controller未校验token|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|page|query|Integer|否|默认1|
|limit|query|Integer|否|默认10|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|list|AiInferenceLog\[\]|日志列表|
|total|Integer|总数|



**请求示例**



```HTTP
GET /api/admin/ml/inference/logs
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "list": [
      {
        "logId": "LOG001",
        "traceId": "AI0123",
        "callSource": "AI_ASSISTED_CONSULT",
        "modelKey": "medical-assistant",
        "modelVersion": "v1",
        "status": "SUCCESS",
        "durationMs": 820,
        "createdAt": "2026-06-14T10:00:00"
      }
    ],
    "total": 1
  }
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.5\.1\.4 训练样本分页



|项目|内容|
|---|---|
|接口地址|`/api/admin/ml/samples/list`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|管理员；当前Controller未校验token|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|page|query|Integer|否|默认1|
|limit|query|Integer|否|默认10|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|list|TrainingSample\[\]|样本列表|
|total|Integer|总数|



**请求示例**



```HTTP
GET /api/admin/ml/samples/list
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "list": [
      {
        "sampleId": "SMP001",
        "datasetName": "consult-feedback",
        "filePath": "/data/sample/1.json",
        "label": "accepted",
        "labelType": "MANUAL",
        "status": "LABELED",
        "createTime": "2026-06-14T10:00:00"
      }
    ],
    "total": 1
  }
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.5\.1\.5 更新训练样本标注



|项目|内容|
|---|---|
|接口地址|`/api/admin/ml/samples/update`|
|请求方式|`POST`|
|请求头|JSON|
|权限说明|管理员；当前Controller未校验token|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|sampleId|body|String|是|样本ID|
|label|body|String|是|标注值|
|labelType|body|String|是|标注类型|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|null|无业务数据|



**请求示例**



```JSON
{
  "sampleId": "SMP001",
  "label": "accepted",
  "labelType": "MANUAL"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": null
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 更新后status固定为LABELED。



##### 2\.5\.1\.6 模型列表



|项目|内容|
|---|---|
|接口地址|`/api/admin/ml/models/list`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|管理员；当前Controller未校验token|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|ModelVersion\[\]|精简模型列表|



**请求示例**



```HTTP
GET /api/admin/ml/models/list
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "modelId": "M001",
      "modelKey": "medical-ct-unet",
      "modelType": "unet",
      "version": "v1.0.0",
      "status": "ACTIVE",
      "createTime": "2026-06-01T10:00:00"
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.5\.1\.7 触发模型训练



|项目|内容|
|---|---|
|接口地址|`/api/admin/ml/models/train`|
|请求方式|`POST`|
|请求头|JSON|
|权限说明|管理员；当前Controller未校验token|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|modelKey|body|String|否|默认medical\-ct\-unet|
|modelType|body|String|否|attention或其他；默认unet|
|datasetPath|body|String|否|默认/data/ct\-artifact/|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|taskId|String|训练任务ID|
|status|String|任务状态|



**请求示例**



```JSON
{
  "modelKey": "medical-ct-unet",
  "modelType": "unet",
  "datasetPath": "/data/ct-artifact/"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "taskId": "TASK001",
    "status": "RUNNING"
  }
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 创建任务后立即异步启动训练。



##### 2\.5\.1\.8 训练任务列表



|项目|内容|
|---|---|
|接口地址|`/api/admin/ml/models/tasks`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|管理员；当前Controller未校验token|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|tasks|Object\[\]|taskId/modelKey/modelType/status/createTime|
|stats|Object|训练器统计|



**请求示例**



```HTTP
GET /api/admin/ml/models/tasks
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "tasks": [
      {
        "taskId": "TASK001",
        "modelKey": "medical-ct-unet",
        "modelType": "UNET",
        "status": "RUNNING",
        "createTime": "2026-06-14T10:00:00"
      }
    ],
    "stats": {
      "running": 1
    }
  }
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。



##### 2\.5\.1\.9 Python推理服务健康检查



|项目|内容|
|---|---|
|接口地址|`/api/admin/ml/python/health`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|管理员；当前Controller未校验token|



**请求参数**



无。



**返回参数**



|参数名|类型|说明|
|---|---|---|
|pythonServiceAlive|Boolean|Python服务是否存活|
|activeModel|String|当前激活模型|



**请求示例**



```HTTP
GET /api/admin/ml/python/health
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "pythonServiceAlive": true,
    "activeModel": "medical-ct-unet v1.0.0"
  }
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 按请求参数查询或处理；空列表返回 `[]`，无业务数据返回 `null`。

##### 2\.5\.1\.10 CT伪影检测推理

|项目|内容|
|---|---|
|接口地址|`/doctor-service/exam/ct-artifact`（检查医生语义路径）|
|兼容地址|`/admin-service/ml/inference/ct-artifact`|
|请求方式|`POST`|
|请求头|multipart/form-data|
|权限说明|业务上归检查医生；当前 Controller 未单独校验 doctorType|

**请求参数**

|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|file|multipart|File|是|CT NIfTI 文件，仅支持 `.nii` 或 `.nii.gz`|

**返回参数**

|参数名|类型|说明|
|---|---|---|
|data|CtArtifactInferenceResult|CT 金属伪影检测结构化结果|

**请求示例**

```HTTP
POST /doctor-service/exam/ct-artifact
multipart/form-data: file=<scan.nii.gz>
```

**返回示例**

```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "status": "success",
    "message": "CT金属伪影检测完成",
    "originalFile": "scan.nii.gz",
    "maskFile": "xxx_scan_mask.nii.gz",
    "shape": [512, 512, 120],
    "spacing": [0.5, 0.5, 1.0],
    "origin": [0.0, 0.0, 0.0],
    "downloadUrl": "/results/xxx_scan_mask.nii.gz",
    "artifactDetected": true,
    "positivePixels": 12034,
    "totalPixels": 31457280,
    "artifactRatio": 0.0383,
    "modelType": "attention",
    "modelVersion": "attention_adamw_e4",
    "summary": "检测到CT金属伪影，伪影像素占比约0.0383%。",
    "reportInput": {
      "task": "CT_ARTIFACT_REPORT",
      "modality": "CT",
      "finding": {
        "artifactDetected": true,
        "positivePixels": 12034,
        "totalPixels": 31457280,
        "artifactRatio": 0.0383,
        "maskFile": "xxx_scan_mask.nii.gz"
      },
      "imageMeta": {
        "shape": [512, 512, 120],
        "spacing": [0.5, 0.5, 1.0],
        "origin": [0.0, 0.0, 0.0]
      },
      "model": {
        "modelType": "attention",
        "modelVersion": "attention_adamw_e4"
      },
      "summary": "检测到CT金属伪影，伪影像素占比约0.0383%。"
    },
    "logId": "INF0123456789abcdef",
    "latencyMs": 842
  }
}
```

**错误码：** 400 文件格式不支持；500 Python 服务不可用或推理异常。

**业务规则：** Python 服务生成掩膜和结构化统计；Java 后端追加 `logId`、`latencyMs` 并记录推理日志。`reportInput` 是后续大语言模型生成文字描述和报告初稿的输入，LLM 不负责重新判断伪影区域。

##### 2\.5\.1\.11 CT伪影检测结果掩膜下载

|项目|内容|
|---|---|
|接口地址|`/doctor-service/exam/ct-artifact/result/{maskFilename}`（检查医生语义路径）|
|兼容地址|`/admin-service/ml/inference/ct-artifact/result/{maskFilename}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|业务上归检查医生；当前 Controller 未单独校验 doctorType|

**请求参数**

|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|maskFilename|path|String|是|推理返回的掩膜文件名，不允许包含 `/` 或 `\`|

**返回参数**

二进制 NIfTI 掩膜文件，`Content-Type: application/octet-stream`。

**请求示例**

```HTTP
GET /doctor-service/exam/ct-artifact/result/xxx_scan_mask.nii.gz
无请求体
```

**业务规则：** 前端通过 Java 后端代理下载，不直接暴露 Python 服务地址。



### 2\.6 支付模块



**模块职责：** 当前仅提供患者支付历史查询，统一支付下单能力尚未实现。



**路由范围：** `/api/payment/**`



**权限说明：** 患者本人或授权内部服务；通过网关调用前需要补充路由配置。



**接口数量：** 1



#### 模块接口清单



|接口名称|请求方式|接口地址|
|---|---|---|
|查询患者支付历史|`GET`|`/api/payment/history/{patientId}`|



#### 模块数据模型



##### `PaymentOrder`



|字段|类型|说明|
|---|---|---|
|orderId|String|支付订单ID|
|patientId|String|患者ID|
|registerId|String|挂号ID|
|payType|String|REGISTER/EXAM/PRESCRIPTION|
|description|String|缴费项目|
|amount|Decimal|金额|
|payStatus|String|WAITING/PAID/CANCELLED/REFUNDED|
|payMethod|String|WECHAT/ALIPAY/CASH|
|payTime|DateTime|支付时间|
|createTime|DateTime|创建时间|



#### 2\.6\.1 支付



##### 2\.6\.1\.1 查询患者支付历史



|项目|内容|
|---|---|
|接口地址|`/api/payment/history/{patientId}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|患者本人/内部服务；当前仅建议直连8005|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|patientId|path|String|是|患者ID|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|PaymentOrder\[\]|支付记录|



**请求示例**



```HTTP
GET /api/payment/history/{patientId}
无请求体
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "orderId": "PAY001",
      "patientId": "p1718000000000ABCD",
      "registerId": "REG001",
      "payType": "REGISTER",
      "description": "挂号费",
      "amount": 30,
      "payStatus": "PAID",
      "payMethod": "WECHAT",
      "payTime": "2026-06-14T09:30:00"
    }
  ]
}
```



**错误码：** 除通用错误码外，无接口专属错误码。



**业务规则：** 网关当前未配置/api/payment/\*\*路由，/payment\-service/\*\*也不会自动去除前缀；通过网关调用前需修复路由。



### 2\.7 内部服务模块



**模块职责：** 提供微服务之间的可信接诊上下文查询，不面向前端或外部调用方。



**路由范围：** `/internal/doctor/**`



**权限说明：** 仅 AI 服务调用；使用医生 ID 和内部共享密钥双重校验，网关屏蔽外部访问。



**接口数量：** 1



#### 模块接口清单



|接口名称|请求方式|接口地址|
|---|---|---|
|获取可信接诊上下文|`GET`|`/internal/doctor/consult/context`|



#### 模块数据模型



##### `ConsultContextDto`



|字段|类型|说明|
|---|---|---|
|available|Boolean|上下文是否可用|
|errorMessage|String|错误说明|
|registerId|String|挂号ID|
|patientId|String|患者ID|
|patientAge|Integer|年龄|
|patientGender|String|性别文本|
|chiefComplaint|String|主诉|
|currentRecordDesc|String|当前病历|
|medicalHistory|String\[\]|历史病历|
|previousReports|String\[\]|历史报告|



#### 2\.7\.1 内部服务



##### 2\.7\.1\.1 获取可信接诊上下文



|项目|内容|
|---|---|
|接口地址|`/internal/doctor/consult/context`|
|请求方式|`GET`|
|请求头|X\-Doctor\-Id \+ X\-Internal\-Service\-Key|
|权限说明|仅AI服务；禁止外部调用|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|query|String|是|挂号ID|
|X\-Doctor\-Id|header|String|是|当前医生ID|
|X\-Internal\-Service\-Key|header|String|是|服务间共享密钥|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|ConsultContextDto|直接返回DTO，不使用Result包裹|



**请求示例**



```HTTP
GET /internal/doctor/consult/context
无请求体
```



**返回示例**



```JSON
{
  "available": true,
  "registerId": "REG001",
  "patientId": "p1718000000000ABCD",
  "patientAge": 35,
  "patientGender": "男",
  "chiefComplaint": "咳嗽三天",
  "currentRecordDesc": "初步病历",
  "medicalHistory": [],
  "previousReports": []
}
```



**错误码：** HTTP 404 密钥缺失或不匹配；500 就诊记录不存在/无权访问



**业务规则：** 不通过网关；网关主动屏蔽/internal/\*\*；密钥采用常量时间比较；同时校验医生拥有该接诊记录。



## 3\. 附录



### 3\.1 已删除或未纳入的原文档内容



|内容|处理|原因|
|---|---|---|
|项目架构、技术栈、目录树、部署说明|删除|不属于 API 接口文档|
|/api/auth/sms/send、/api/auth/logout|删除|当前无 Controller 映射|
|管理员账号、排班、药品接口|删除|对应 Controller 为空|
|AI 智能分诊|删除|对应 Controller 为空；AI生成检查/检验项目建议和AI报告分析已纳入 2.4 AI 能力模块|
|统一支付 /api/pay/unified\-pay|删除|当前 payment\-service 仅实现支付历史查询|
|医生头像上传、修改手机、修改密码|未纳入正式清单|已有路由但为固定返回或 TODO，不能视为已实现|
|原前端 MLOps 单数路径|替换|后端真实路径为 `/samples/**` 和 `/models/**`|



### 3\.2 联调前必须修复项



1. 实现 Gateway 全局过滤器，至少保证返回 `chain.filter(exchange)`，并完成 JWT 角色校验。

2. 为 `/api/payment/**` 配置网关路由，或统一 Controller 与网关的 `/payment-service/**` 前缀并增加 `StripPrefix`。

3. 将患者病历/处方 `my-list` 接口改为从 token 读取 patientId，禁止客户端任意指定。

4. 为接诊详情、草稿、检查单和完成接诊补充记录归属校验。

5. 为 `/api/admin/**` 补充管理员角色校验，并同步修正前端 MLOps 请求路径。

6. 完成头像文件持久化、短信验证码验证和医生资料相关 TODO 后，再补充对应正式接口。

7. 统一 BusinessException 的业务 code 与 HTTP 状态，避免所有业务错误都表现为 code=500。
