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
|payment\-service|8005|/api/payment/\*\*、/payment-service/pay/\*\*|



Gateway 默认端口为 `80`，基地址为 `http://{gateway-host}`。



### 1\.2 请求与响应



|项目|约定|
|---|---|
|JSON 请求|`Content-Type: application/json`|
|文件上传|`Content-Type: multipart/form-data`|
|登录令牌|请求头 `token`；当前代码未使用 `Authorization: Bearer`|
|成功响应|`{"code":200,"msg":"成功","data":...}`|
|空数据|列表返回 `[]`；无业务数据通常返回 `null`|
|特殊响应|AI 药品问答接口返回 `text/event-stream`，不使用统一 Result 包裹；其余当前 AI 对外业务接口通常使用统一 Result 包裹；MLOps 掩膜/预览下载接口返回二进制文件响应|
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
|权限说明|患者本人；当前实现从患者 token 解析 patientId，并仅返回该患者名下 registerId 对应的病历|



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
GET /api/patient/medical/list?registerId=REG001
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
|权限说明|患者本人；当前实现从患者 token 解析 patientId，并仅返回该患者名下 registerId 对应的处方|



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
GET /api/patient/prescription/list?registerId=REG001
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
|确认医技申请并进入队列|`POST`|`/doctor-service/consult/medical-order/confirm`|
|查询本次检查/检验报告|`GET`|`/doctor-service/consult/reports`|
|完成接诊|`POST`|`/doctor-service/consult/complete`|
|开具处方|`POST`|`/doctor-service/consult/create-prescription`|
|按挂号ID查询处方|`GET`|`/doctor-service/consult/prescription-list`|
|按挂号ID查询检查单|`GET`|`/doctor-service/exam-order/list`|
|查询当前医生开具的检查单|`GET`|`/doctor-service/exam-order/my-list`|
|检查/检验医生提交报告|`POST`|`/doctor-service/task/report`|
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
|请求头|token: 医生JWT（必填）|
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
|reportReturnedOnly|query|Boolean|否|是否仅返回已有检查/检验报告回传的接诊记录，默认 false|
|page|query|Integer|否|默认1|
|limit|query|Integer|否|默认10|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|ConsultRecord\[\]|接诊列表|

`ConsultRecord` 在接诊列表中额外返回 `reportCount`、`latestReportTime` 和 `hasReturnedReport`，用于接诊工作台展示“报告已回传/待复诊分析”提醒。`reportReturnedOnly=true` 时在服务端按已发布医技报告过滤后再分页。



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



**业务规则：** 校验当前医生拥有该就诊记录；新增病历时后端回填 `doctorName`、`patientName`、`visitAge`、`visitDate`、`payStatus` 和 `createTime`，其中 `doctorName` 来自接诊医生信息，`payStatus` 继承挂号记录支付状态；更新病历时仅更新病历内容；保存后将接诊状态标记为IN\_PROGRESS。



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



**业务规则：** 校验当前医生拥有该就诊记录；新增病历时后端回填 `doctorName`、`patientName`、`visitAge`、`visitDate`、`payStatus` 和 `createTime`，其中 `doctorName` 来自接诊医生信息，`payStatus` 继承挂号记录支付状态；更新病历时仅更新病历内容；确认后将接诊状态改为RECORD\_CONFIRMED。



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



**业务规则：** 该接口保留兼容旧前端。当前接诊主流程建议使用 `POST /doctor-service/consult/medical-order/confirm`，按 `medical_item.item_code` 确认正式医技申请并直接推进检查/检验队列。



##### 2\.3\.2\.5A 确认医技申请并进入队列

|项目|内容|
|---|---|
|接口地址|`/doctor-service/consult/medical-order/confirm`|
|请求方式|`POST`|
|请求头|token: 接诊医生JWT（必填）|
|权限说明|仅接诊医生，且必须拥有该挂号记录|

**请求参数**

|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|挂号ID|
|aiTraceId|body|String|否|AI检查/检验建议 traceId；传入时会校验提交项目来自本次AI建议|
|clinicalSummary|body|String|是|临床摘要，可使用当前病历内容|
|urgencyLevel|body|String|否|NORMAL、URGENT、EMERGENCY|
|items|body|MedicalOrderItemRequest\[\]|是|待确认的正式医技项目，最多5项|
|items\[\].itemCode|body|String|是|`medical_item.item_code`，必须在系统支持枚举中|
|items\[\].urgencyLevel|body|String|否|单项目紧急程度；缺省继承主申请|

**返回参数**

|参数名|类型|说明|
|---|---|---|
|orderId|String|医技申请主单ID|
|sourceType|String|AI_ASSISTED 或 MANUAL|
|itemCount|Integer|项目数量|
|totalAmount|Decimal|项目总金额|
|status|String|当前开发联调流返回 QUEUED|
|payStatus|String|当前开发联调流返回 PAID|
|queueReady|Boolean|是否已进入检查/检验队列|
|paymentMessage|String|支付服务不可用时的非阻断提示|

**请求示例**

```JSON
{
  "registerId": "REG001",
  "aiTraceId": "AI202607050001",
  "clinicalSummary": "患者头痛伴眩晕三天，需排除颅内病变。",
  "urgencyLevel": "NORMAL",
  "items": [
    { "itemCode": "CRANIAL_CT_PLAIN", "urgencyLevel": "NORMAL" }
  ]
}
```

**返回示例**

```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "orderId": "MO...",
    "sourceType": "AI_ASSISTED",
    "itemCount": 1,
    "totalAmount": 280.00,
    "status": "QUEUED",
    "payStatus": "PAID",
    "queueReady": true,
    "paymentMessage": null
  }
}
```

**业务规则：** 当前开发联调默认采用“生成即入队”：后端仍会尝试调用 payment-service 创建 `orderType=MEDICAL` 支付单，但支付服务失败或超时不阻断接诊主流程；doctor-service 会将 `medical_order.pay_status` 推进到 `PAID`，将 `medical_order.status` 与相关 `medical_order_item.status` 推进到 `QUEUED`，使检查/检验医生工作台可立即读取任务。支付失败只通过 `paymentMessage` 返回提示。



##### 2\.3\.2\.5B 查询本次检查/检验报告

|项目|内容|
|---|---|
|接口地址|`/doctor-service/consult/reports`|
|请求方式|`GET`|
|请求头|token: 接诊医生JWT（必填）|
|权限说明|仅接诊医生，且必须拥有该挂号记录|

**请求参数**

|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|query|String|是|挂号ID|

**返回参数**

|参数名|类型|说明|
|---|---|---|
|data|MedicalReportVo\[\]|本次挂号已发布的检查/检验报告|

**业务规则：** 仅返回 `medical_report.status=PUBLISHED` 的报告。接诊医生报告分析页优先读取该接口返回内容，仍可保留手动粘贴报告作为补充输入。



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
|请求头|token: 医生JWT（必填）|
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



**业务规则：** 当前实现创建处方并将 `payStatus` 固定为 `WAITING`，同时同步调用 payment-service 创建 `orderType=PRESCRIPTION`、`businessId=prescriptionId` 的待支付订单，支付金额按单价 `price` × 数量 `num` 计算；返回体会回填 `payStatus` 和 `createTime`；后端从所属接诊记录回填 `patientId`、`patientName` 和 `doctorName`，不信任前端传入这些展示字段；`medicineId` 为兼容新增字段，传入时写入 `prescription.medicine_id`，旧调用不传仍可创建处方；开方本身不扣减药品库存。



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
|请求头|token: 医生JWT（必填）|
|权限说明|医生本人；doctorId 从 token 解析，客户端传入 doctorId 会被忽略|



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
GET /doctor-service/exam-order/list?registerId=REG001
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



**模块职责：** 提供统一 AI 聊天入口、AI 辅助接诊/辅助诊断、AI 病历草稿生成、AI 处方审核、AI 药品问答、AI 报告分析、AI 智能问诊推荐、AI 检查检验建议和 AI 智能排班。



**路由范围：** `/ai-service/**`



**模型配置：** AI 聊天、接诊、病历生成、处方审核、报告分析、检查建议、药品问答和智能排班等大模型能力通过阿里云百炼 OpenAI 兼容接口调用 `deepseek-v4-flash`；接诊聊天和药品问答中的药品知识参考/PDF 知识库检索使用阿里云百炼向量模型 `text-embedding-v4`，维度配置为 `1024`。当前项目使用 Spring AI OpenAI 适配器，框架会自动追加 `/v1/chat/completions` 和 `/v1/embeddings`，因此后端配置为 `spring.ai.openai.base-url=``https://dashscope.aliyuncs.com/compatible-mode`、`spring.ai.openai.chat.options.model=deepseek-v4-flash`、`spring.ai.embedding.model=text-embedding-v4`，API Key 建议通过环境变量 `DASHSCOPE_API_KEY` 或 Nacos 的 `ai-service.yml` 下发。



**权限说明：** 辅助接诊、病历生成、处方审核和报告分析接口仅医生使用；AI 智能排班接口仅管理员使用；智能问诊推荐、检查建议和药品问答接口当前 Controller 未显式校验 token。AI 接诊类接口会通过 doctor\-service 内部接口校验医生与挂号记录归属。



**接口数量： 11（服务间内部依赖接口见 2\.7 内部服务模块）**



#### 模块接口清单



|接口名称|请求方式|接口地址|
|---|---|---|
|统一AI聊天/辅助接诊/专业模块编排|`POST`|`/ai-service/reception/chat`|
|AI病历草稿生成|`POST`|`/ai-service/reception/record/generate`|
|AI药品问答流式输出|`POST`|`/ai-service/medicine/chat`|
|AI处方审核|`POST`|`/ai-service/prescription/review`|
|AI报告分析|`POST`|`/ai-service/report/analyze`|
|AI智能问诊推荐|`POST`|`/ai-service/consult/recommend`|
|获取推荐科室列表|`GET`|`/ai-service/consult/departments`|
|AI检查检验建议生成|`POST`|`/ai-service/agent/exam/generate`|
|AI排班预览|`POST`|`/ai-service/schedule/preview`|
|AI排班冲突检查|`POST`|`/ai-service/schedule/conflict-check`|
|AI排班发布|`POST`|`/ai-service/schedule/publish`|



#### 模块数据模型



##### `AiAssistantChatRequest`



|字段|类型|说明|
|---|---|---|
|registerId|String|当前接诊挂号ID，必填，最多32字符|
|message|String|医生在AI对话框中输入的自然语言问题，最多2000字符；message和actionType至少提供一项|
|actionType|String|前端按钮指定的 AI 能力；未传或无法识别时后端会按医生文本关键词做保守匹配，最多64字符|
|medicineId|String|可选药品ID，用于统一接诊聊天中补充药品知识上下文，最多32字符|
|currentRecordDesc|String|医生当前正在编辑的病历草稿，优先于数据库旧草稿，最多10000字符|
|symptomDescription|String|患者症状补充描述，最多5000字符|
|conversationText|String|医患对话原文，主要供AI病历自动生成模块整理病历，最多20000字符|
|structuredParameters|Map\<String,String\>|结构化问诊参数，最多30项|
|followUpAnswers|Map\<String,String\>|医生已完成的AI追问及患者回答，最多10项|
|patientInformation|Map\<String,String\>|处方审核需要的补充患者信息，例如过敏史、妊娠状态等，最多30项|
|medicines|PrescriptionReviewMedicineRequest\[\]|处方审核场景下待审核的药品列表，最多10项；统一接诊聊天也可据此补充药品知识上下文|



`actionType` 支持值来自当前源码的 `AiAssistantIntentEnum`：`FOLLOW_UP_QUESTION`、`MISSING_INFORMATION`、`CONTEXT_SUMMARY`、`CONTEXT_QA`、`MEDICAL_RECORD_DRAFT`、`PRESCRIPTION_REVIEW`、`DIAGNOSIS_ASSISTANT`、`UNKNOWN`。快捷按钮传入的 `actionType` 优先级最高；未传或无法识别时，后端会按医生文本关键词做保守匹配，未命中则默认 `CONTEXT_QA`。`MEDICAL_RECORD_DRAFT`、`PRESCRIPTION_REVIEW` 分别委派给病历草稿生成和处方审核专业模块。



##### `AiAssistantChatResponse`





|字段|类型|说明|
|---|---|---|
|traceId|String|本次AI请求追踪ID；当前聊天直接回答场景可能为空|
|intent|String|识别出的意图，对应 `AiAssistantIntentEnum`|
|answer|String|可直接展示在医生AI对话框中的文本回复|
|status|String|SUCCESS/DELEGATED/NEEDS\_INPUT/FAILED/UNSUPPORTED|
|handledModule|String|实际处理模块；病历生成为 `AI_MEDICAL_RECORD`，处方审核为 `AI_PRESCRIPTION_REVIEW`|
|moduleResult|Object|专业模块结构化结果，例如病历草稿或处方审核结果|
|modelVersion|String|当前使用的大模型版本或模型标识|
|handledByAssistant|Boolean|是否由AI辅助接诊模块自身处理|
|fallback|Boolean|是否为异常降级结果|





##### `AiRecordGenerateRequest`



|字段|类型|说明|
|---|---|---|
|registerId|String|当前接诊挂号ID，必填，最多32字符|
|conversationText|String|医患对话原文，最多20000字符；conversationText和structuredParameters至少提供一项|
|structuredParameters|Map\<String,String\>|结构化问诊参数，最多30项|
|currentRecordDesc|String|医生当前病历草稿，最多10000字符|



##### `AiRecordGenerateResponse`



|字段|类型|说明|
|---|---|---|
|traceId|String|本次生成追踪ID；当前服务实现可能为空|
|status|String|SUCCESS/FAILED|
|modelVersion|String|模型名称或版本|
|informationCompleteness|String|SUFFICIENT/INCOMPLETE|
|structuredRecord|AiStructuredMedicalRecord|结构化病历字段|
|draftRecordDesc|String|供医生编辑确认的完整病历草稿|
|missingInformation|String\[\]|仍需补充的信息|
|riskLevel|String|LOW/MEDIUM/HIGH/CRITICAL/UNKNOWN|
|riskWarnings|String\[\]|由现有信息支持的风险提示|
|fallback|Boolean|是否降级结果|



##### `AiStructuredMedicalRecord`



|字段|类型|说明|
|---|---|---|
|chiefComplaint|String|主诉|
|historyOfPresentIllness|String|现病史|
|pastMedicalHistory|String|既往史|
|allergyHistory|String|过敏史|
|personalHistory|String|个人史|
|familyHistory|String|家族史|
|physicalExamination|String|体格检查|
|auxiliaryExamination|String|辅助检查|
|assessment|String|初步诊断与依据|
|treatmentPlan|String|处理计划|



##### `PrescriptionReviewRequest`



|字段|类型|说明|
|---|---|---|
|registerId|String|当前接诊挂号ID，必填，最多32字符|
|currentRecordDesc|String|医生当前病历草稿，最多10000字符|
|patientInformation|Map\<String,String\>|患者补充信息，例如过敏史、妊娠状态等，最多30项|
|medicines|PrescriptionReviewMedicineRequest\[\]|待审核药品列表，必填，最多10项|



##### `PrescriptionReviewMedicineRequest`



|字段|类型|说明|
|---|---|---|
|medicineId|String|药品ID，必填，最多32字符|
|usage|String|医嘱用法，必填，最多200字符|
|quantity|Integer|数量，必填，范围1\-10000|



##### `PrescriptionReviewResponse`



|字段|类型|说明|
|---|---|---|
|traceId|String|本次审核追踪ID；当前服务实现可能为空|
|status|String|SUCCESS/FAILED|
|modelVersion|String|模型名称或版本|
|passed|Boolean|是否通过审核|
|overallRiskLevel|String|LOW/MEDIUM/HIGH/CRITICAL/UNKNOWN|
|summary|String|总体审核结论|
|interactions|DrugInteractionResult\[\]|药物相互作用结果|
|medicineRisks|PrescriptionMedicineRisk\[\]|单药风险结果|
|contraindications|String\[\]|禁忌或潜在禁忌|
|recommendations|String\[\]|总体用药建议|
|missingInformation|String\[\]|审核所缺信息|
|fallback|Boolean|是否降级结果|



##### `DrugInteractionResult`



|字段|类型|说明|
|---|---|---|
|medicineA|String|药品A名称|
|medicineB|String|药品B名称|
|severity|String|LOW/MEDIUM/HIGH/CRITICAL/UNKNOWN|
|description|String|相互作用说明|
|recommendation|String|处理建议|



##### `PrescriptionMedicineRisk`



|字段|类型|说明|
|---|---|---|
|medicineId|String|药品ID|
|medicineName|String|药品名称|
|riskLevel|String|LOW/MEDIUM/HIGH/CRITICAL/UNKNOWN|
|issues|String\[\]|该药品的具体风险|
|suggestions|String\[\]|调整或监测建议|



##### `MedicineQueryDto`



|字段|类型|说明|
|---|---|---|
|sessionId|String|药品问答会话ID；用于 Redis 保存最近对话历史|
|question|String|医生或患者输入的药品问题|
|medicineId|String|可选药品ID；传入时优先按ID查询药品库|
|userRole|String|用户角色，`doctor` 或 `patient`；为空或非 patient 时默认按医生问答提示词处理|
|patientId|String|患者ID；患者端调用时可携带，后端会用于区分会话历史 key|



##### `ReportAnalysisDto`



|字段|类型|说明|
|---|---|---|
|registerId|String|挂号ID，必填，最多32字符|
|reportType|String|报告类型；普通报告可传 EXAM/LAB，CT 结构化结果建议传 CT_ARTIFACT_REPORT 或 CT_LESION_REPORT|
|reportText|String|检查/检验报告原文；与 indicators、reportInput 至少提供一项|
|indicators|IndicatorDto\[\]|检验指标明细；与 reportText、reportInput 至少提供一项|
|reportInput|Map\<String,Object\>|影像 AI 标准化 JSON 结果；用于承接 CT 金属伪影识别、CT 病灶识别与分割接口输出的 `reportInput`|



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
|riskLevel|String|风险等级，例如 LOW/MEDIUM/HIGH|
|abnormalIndicators|AbnormalIndicatorVo\[\]|异常指标解释；CT 结构化结果场景可为空数组|
|suggestions|String\[\]|处理建议|
|followUpAdvice|String|随访或复查建议|
|fallback|Boolean|是否为降级结果|



##### `AbnormalIndicatorVo`



|字段|类型|说明|
|---|---|---|
|name|String|指标名称|
|value|String|指标值|
|referenceRange|String|参考范围|
|interpretation|String|AI解释|



##### `ReportContextDto`



|字段|类型|说明|
|---|---|---|
|available|Boolean|上下文是否可用|
|errorMessage|String|不可用原因|
|registerId|String|挂号ID|
|patientId|String|患者ID|
|patientAge|Integer|患者年龄|
|patientGender|String|患者性别文本|
|chiefComplaint|String|挂号主诉|
|currentRecordDesc|String|当前病历描述|
|medicalHistory|String\[\]|历史病历摘要|
|previousReports|String\[\]|历史检查/检验报告摘要|



##### `AiScheduleGenerateRequest`



|字段|类型|说明|
|---|---|---|
|doctorId|String|医生ID，必填，最多32字符|
|doctorName|String|医生姓名，必填，最多64字符|
|deptId|String|科室ID，必填，最多32字符|
|periodStart|Date|排班开始日期，必填|
|periodEnd|Date|排班结束日期，必填；不能早于periodStart|
|requirement|String|管理员自然语言排班要求，最多1000字符|
|defaultMaxNum|Integer|默认号源数，1\-500，默认30|
|defaultPrice|Decimal|默认挂号费，不能小于0，默认0\.00|
|rooms|String\[\]|可用诊室，最多20项，每项最多64字符|
|timeWindows|AiScheduleTimeWindow\[\]|可用时间窗口，最多30项；为空时使用工作日默认上午/下午窗口|
|unavailableDates|Date\[\]|不可排班日期，最多60项|



##### `AiScheduleTimeWindow`



|字段|类型|说明|
|---|---|---|
|dayOfWeek|Integer|星期几，1\-7表示周一到周日；为空表示请求周期内每天都适用|
|startTime|Time|开始时间|
|endTime|Time|结束时间；必须晚于startTime才会作为有效窗口|
|maxNum|Integer|该窗口号源数，1\-500；为空时使用defaultMaxNum|
|price|Decimal|该窗口挂号费；为空时使用defaultPrice|
|room|String|诊室，最多64字符|



##### `AiScheduleItem`



|字段|类型|说明|
|---|---|---|
|doctorId|String|医生ID，必填，最多32字符|
|doctorName|String|医生姓名，必填，最多64字符|
|deptId|String|科室ID，必填，最多32字符|
|workDate|Date|工作日期，必填|
|startTime|Time|开始时间，必填|
|endTime|Time|结束时间，必填|
|maxNum|Integer|号源数，必填，1\-500|
|price|Decimal|挂号费，必填，不能小于0|
|room|String|诊室，最多64字符|
|conflict|Boolean|是否存在排班冲突|
|conflictType|String|冲突类型；常见值 `DOCTOR_TIME` 医生时间冲突、`ROOM_TIME` 诊室占用冲突、`BATCH_DOCTOR_TIME` 本批次医生冲突、`BATCH_ROOM_TIME` 本批次诊室冲突、`CHECK_FAILED`/`CHECK_EXCEPTION` 冲突检查失败|
|conflictReason|String|冲突原因，最多200字符|



##### `AiScheduleGenerateResponse`



|字段|类型|说明|
|---|---|---|
|traceId|String|本次生成追踪ID；当前服务实现可能为空|
|status|String|SUCCESS/FALLBACK|
|modelVersion|String|模型名称或版本|
|summary|String|排班策略摘要|
|items|AiScheduleItem\[\]|排班草稿项，最多100项|
|warnings|String\[\]|生成、读取现有排班或冲突检查时的提示|
|optimizationReasons|String\[\]|排班优化理由|
|fallback|Boolean|是否为规则降级生成|



##### `AiScheduleConflictCheckRequest`



|字段|类型|说明|
|---|---|---|
|items|AiScheduleItem\[\]|待检查排班项，必填，最多100项|



##### `AiSchedulePublishRequest`



|字段|类型|说明|
|---|---|---|
|traceId|String|排班草稿追踪ID，最多64字符|
|items|AiScheduleItem\[\]|待发布排班项，必填，最多100项|



##### `AiSchedulePublishResponse`



|字段|类型|说明|
|---|---|---|
|traceId|String|发布追踪ID；当前服务实现可能为空|
|status|String|SUCCESS/PARTIAL\_SUCCESS/FAILED|
|submittedCount|Integer|提交的排班项数量|
|createdCount|Integer|实际创建成功数量|
|createdSchedules|DoctorSchedule\[\]|admin\-service 返回的已创建排班|
|failedItems|SchedulePublishFailure\[\]|未创建的排班项及失败原因；例如医生已停职、医生时间冲突、诊室占用冲突或本批次内部冲突|
|warnings|String\[\]|发布失败、部分成功或冲突提示|



##### `SchedulePublishFailure`



|字段|类型|说明|
|---|---|---|
|index|Integer|失败项在发布请求 `items` 数组中的下标，从0开始|
|doctorId|String|医生ID|
|doctorName|String|医生姓名|
|workDate|Date|出诊日期|
|startTime|Time|出诊开始时间|
|endTime|Time|出诊结束时间|
|room|String|诊室|
|reason|String|失败原因|
|conflictType|String|冲突类型或失败类型；常见值同 `AiScheduleItem.conflictType`|



#### 2\.4\.1 AI接诊聊天与专业模块编排



##### 2\.4\.1\.1 AI辅助接诊聊天



|项目|内容|
|---|---|
|接口地址|`/ai-service/reception/chat`|
|请求方式|`POST`|
|请求头|token: 医生JWT（必填）|
|权限说明|医生本人；通过内部接口校验医生与挂号记录归属|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|当前接诊挂号ID|
|message|body|String|条件必填|医生自然语言问题；与actionType至少提供一项|
|actionType|body|String|条件必填|前端按钮选择的 AI 能力；与message至少提供一项；未传或无法识别时后端会按医生文本关键词做保守匹配|
|medicineId|body|String|否|可选药品ID，用于接诊聊天中补充药品知识上下文|
|currentRecordDesc|body|String|否|医生当前正在编辑的病历草稿|
|symptomDescription|body|String|否|患者症状补充描述|
|conversationText|body|String|否|医患对话原文；病历生成委派时会优先使用|
|structuredParameters|body|Map\<String,String\>|否|结构化问诊参数|
|followUpAnswers|body|Map\<String,String\>|否|已完成的追问及患者回答|
|patientInformation|body|Map\<String,String\>|否|处方审核补充患者信息|
|medicines|body|PrescriptionReviewMedicineRequest\[\]|否|处方审核时必填；接诊聊天可用于补充药品知识上下文|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|AiAssistantChatResponse|AI对话回复或专业模块编排结果|



**请求示例：AI追问建议**



```JSON
{
  "registerId": "REG001",
  "actionType": "FOLLOW_UP_QUESTION",
  "message": "这个患者下一步还需要追问哪些问题？",
  "currentRecordDesc": "主诉：咳嗽三天。",
  "followUpAnswers": {
    "是否发热？": "否"
  }
}
```



**返回示例：AI追问建议**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "traceId": null,
    "intent": "FOLLOW_UP_QUESTION",
    "answer": "建议继续确认：1. 咳嗽性质；2. 是否咳痰；3. 是否胸闷气促；4. 是否接触呼吸道感染患者。",
    "status": "SUCCESS",
    "handledModule": null,
    "moduleResult": null,
    "modelVersion": "deepseek-v4-flash",
    "handledByAssistant": true,
    "fallback": false
  }
}
```



**请求示例：委派AI病历生成**



```JSON
{
  "registerId": "REG001",
  "actionType": "MEDICAL_RECORD_DRAFT",
  "conversationText": "患者诉咳嗽三天，夜间加重，无发热。",
  "structuredParameters": {
    "咳嗽持续时间": "3天",
    "发热": "否"
  },
  "currentRecordDesc": "主诉：咳嗽三天。"
}
```



**返回示例：委派AI病历生成**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "traceId": null,
    "intent": "MEDICAL_RECORD_DRAFT",
    "answer": "已调用AI病历自动生成模块生成病历草稿。",
    "status": "DELEGATED",
    "handledModule": "AI_MEDICAL_RECORD",
    "moduleResult": {
      "traceId": null,
      "status": "SUCCESS",
      "modelVersion": "deepseek-v4-flash",
      "informationCompleteness": "INCOMPLETE",
      "draftRecordDesc": "主诉：咳嗽3天。现病史：患者咳嗽3天，夜间加重，无发热。",
      "riskLevel": "LOW",
      "riskWarnings": [],
      "fallback": false
    },
    "modelVersion": "deepseek-v4-flash",
    "handledByAssistant": false,
    "fallback": false
  }
}
```



**错误码：** 400 参数校验失败；500/业务异常 token无效、非医生角色、挂号记录不存在、无权访问、内部服务密钥缺失；200\+FAILED/NEEDS\_INPUT 表示模型降级或委派模块缺少必要输入



**业务规则：** 先通过token解析医生ID并限制医生角色；再调用 `/internal/doctor/consult/context` 获取可信患者上下文。快捷按钮传入的 `actionType` 优先级最高；未传或无法识别时，后端会按医生文本关键词做保守匹配，未命中则默认 `CONTEXT_QA`。`FOLLOW_UP_QUESTION`、`MISSING_INFORMATION`、`CONTEXT_SUMMARY`、`CONTEXT_QA` 和 `DIAGNOSIS_ASSISTANT` 由 AI 辅助接诊模块直接处理；`MEDICAL_RECORD_DRAFT` 委派AI病历草稿生成；`PRESCRIPTION_REVIEW` 委派AI处方审核，调用时必须提供真实 `medicineId`、`usage`、`quantity`。正式处方仍需医生审核后通过医生接诊业务接口提交。





##### 2\.4\.1\.2 AI病历草稿生成



|项目|内容|
|---|---|
|接口地址|`/ai-service/reception/record/generate`|
|请求方式|`POST`|
|请求头|token: 医生JWT（必填）|
|权限说明|医生本人；通过内部接口校验医生与挂号记录归属|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|当前接诊挂号ID|
|conversationText|body|String|条件必填|医患对话原文；与structuredParameters至少提供一项|
|structuredParameters|body|Map\<String,String\>|条件必填|结构化问诊参数；与conversationText至少提供一项|
|currentRecordDesc|body|String|否|医生当前病历草稿；优先于数据库旧草稿|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|AiRecordGenerateResponse|AI生成的结构化病历和病历草稿|



**请求示例**



```JSON
{
  "registerId": "REG001",
  "conversationText": "医生：哪里不舒服？患者：咳嗽三天，夜里明显，没有发热。",
  "structuredParameters": {
    "咳嗽持续时间": "3天",
    "夜间加重": "是",
    "发热": "否"
  },
  "currentRecordDesc": "主诉：咳嗽三天。"
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "traceId": null,
    "status": "SUCCESS",
    "modelVersion": "deepseek-v4-flash",
    "informationCompleteness": "INCOMPLETE",
    "structuredRecord": {
      "chiefComplaint": "咳嗽3天",
      "historyOfPresentIllness": "患者咳嗽3天，夜间明显，无发热。",
      "pastMedicalHistory": "待补充",
      "allergyHistory": "待补充",
      "personalHistory": "待补充",
      "familyHistory": "待补充",
      "physicalExamination": "待补充",
      "auxiliaryExamination": "待补充",
      "assessment": "待医生结合查体和检查确认",
      "treatmentPlan": "待补充"
    },
    "draftRecordDesc": "主诉：咳嗽3天。现病史：患者咳嗽3天，夜间明显，无发热。既往史：待补充。",
    "missingInformation": [
      "咳嗽性质",
      "是否咳痰",
      "既往史",
      "过敏史"
    ],
    "riskLevel": "LOW",
    "riskWarnings": [],
    "fallback": false
  }
}
```



**错误码：** 400 参数校验失败；500/业务异常 token无效、非医生角色、挂号记录不存在、无权访问、内部服务密钥缺失；200\+FAILED 表示AI病历生成降级



**业务规则：** 该接口是 AI 病历草稿生成模块的直接入口；统一聊天入口也可通过 `actionType=MEDICAL_RECORD_DRAFT` 委派调用同一模块。服务只生成可编辑草稿，不直接保存正式病历。生成前从 doctor\-service 拉取患者年龄、性别、主诉、当前病历、历史病历和历史报告；请求中的 `currentRecordDesc` 会覆盖上下文里的旧草稿。模型失败时返回 `status=FAILED`、`fallback=true`，并尽量回填当前病历草稿。



##### 2\.4\.1\.3 AI处方审核



|项目|内容|
|---|---|
|接口地址|`/ai-service/prescription/review`|
|请求方式|`POST`|
|请求头|token: 医生JWT（必填）|
|权限说明|医生本人；通过内部接口校验医生与挂号记录归属|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|当前接诊挂号ID|
|currentRecordDesc|body|String|否|医生当前病历草稿|
|patientInformation|body|Map\<String,String\>|否|患者补充信息，例如过敏史、妊娠状态、肝肾功能等|
|medicines|body|PrescriptionReviewMedicineRequest\[\]|是|待审核药品列表，最多10项|
|medicines\[\]\.medicineId|body|String|是|药品ID，必须存在于药品库|
|medicines\[\]\.usage|body|String|是|医嘱用法|
|medicines\[\]\.quantity|body|Integer|是|数量，1\-10000|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|PrescriptionReviewResponse|AI处方安全审核结果|



**请求示例**



```JSON
{
  "registerId": "REG001",
  "currentRecordDesc": "主诉：咳嗽三天。无发热。青霉素过敏史待确认。",
  "patientInformation": {
    "过敏史": "青霉素过敏",
    "妊娠状态": "否"
  },
  "medicines": [
    {
      "medicineId": "MED001",
      "usage": "口服，一次1片，一日3次",
      "quantity": 12
    }
  ]
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "traceId": null,
    "status": "SUCCESS",
    "modelVersion": "deepseek-v4-flash",
    "passed": false,
    "overallRiskLevel": "MEDIUM",
    "summary": "存在需医生确认的用药风险，建议补充过敏史和适应证信息后再开具。",
    "interactions": [],
    "medicineRisks": [
      {
        "medicineId": "MED001",
        "medicineName": "示例药品",
        "riskLevel": "MEDIUM",
        "issues": [
          "患者存在相关过敏史信息，需核对禁忌"
        ],
        "suggestions": [
          "确认过敏史后选择替代药物或调整处方"
        ]
      }
    ],
    "contraindications": [
      "青霉素过敏史需确认"
    ],
    "recommendations": [
      "由医生或药师复核后再提交处方"
    ],
    "missingInformation": [
      "明确过敏反应类型"
    ],
    "fallback": false
  }
}
```



**错误码：** 400 参数校验失败；500/业务异常 token无效、非医生角色、挂号记录不存在、无权访问、内部服务密钥缺失、药品不存在、处方药品重复；200\+FAILED 表示AI处方审核降级



**业务规则：** 审核前会按 `medicineId` 查询药品库，并拒绝重复药品或不存在的药品。AI结合患者上下文、当前病历、历史病历、历史报告、患者补充信息和药品库中的用法/适应证/注意事项生成审核结论；模型输出后还会做确定性校验和字段规范化。审核结果只作为医生或药师辅助意见，不直接提交或修改处方。



##### 2\.4\.1\.4 AI药品问答流式输出



|项目|内容|
|---|---|
|接口地址|`/ai-service/medicine/chat`|
|请求方式|`POST`|
|请求头|`Content-Type: application/json`；`Accept: text/event-stream`|
|权限说明|当前 Controller 未显式校验 token；医生端 `/doctor/ai-medicine` 页面直接调用该 SSE 接口|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|sessionId|body|String|是|药品问答会话ID；用于保存最近对话历史|
|question|body|String|是|医生或患者输入的药品问题|
|medicineId|body|String|否|药品ID；传入时优先按ID查询药品库|
|userRole|body|String|否|用户角色，`doctor` 或 `patient`；默认 `doctor`|
|patientId|body|String|否|患者ID；患者端调用时可携带，用于区分会话历史 key|



**返回参数**



该接口返回 `Content-Type: text/event-stream` 的流式文本片段，响应体不是统一 `Result` JSON。



**请求示例**



```JSON
{
  "sessionId": "medicine-session-001",
  "question": "阿莫西林和布洛芬可以一起用吗？",
  "medicineId": "MED001",
  "userRole": "doctor"
}
```



**返回示例**



```text
阿莫西林和布洛芬通常不存在明确的严重相互作用...
```



**错误码：** SSE 建连失败或模型/向量检索异常时连接会结束或返回异常；前端需按流式请求失败处理。



**业务规则：** 后端创建 `SseEmitter(300000L)`，根据 `medicineId` 或问题关键词查询药品库，并结合向量知识库相似文档生成回答。`userRole=patient` 时使用患者友好提示词；其他情况按医生临床药学顾问提示词处理。最近 5 轮 User/Assistant 对话会写入 Redis，默认 30 分钟过期。



#### 2\.4\.2 AI智能排班



**统一响应外层字段**



|参数名|类型|说明|
|---|---|---|
|code|Integer|统一业务状态码；`200` 表示请求被后端正常处理，具体业务结果仍需继续查看 `data.status`、`data.fallback` 和 `data.warnings`|
|msg|String|统一响应提示语；成功通常为 `成功`|
|data|Object|接口业务数据；预览和冲突检查为 `AiScheduleGenerateResponse`，发布为 `AiSchedulePublishResponse`|



**AiScheduleTimeWindow 字段说明**



|参数名|类型|必填|说明|
|---|---|---|---|
|dayOfWeek|Integer|否|星期限制；`1-7` 分别表示周一到周日；不传或为 `null` 表示请求周期内每天都可使用该时间窗口|
|startTime|Time|否|时间窗口开始时间，格式 `HH:mm:ss`；例如 `08:30:00`|
|endTime|Time|否|时间窗口结束时间，格式 `HH:mm:ss`；必须晚于 `startTime` 才会被视为有效排班时段|
|maxNum|Integer|否|该时间窗口的号源数量；未传时使用请求体中的 `defaultMaxNum`；范围 `1-500`|
|price|Decimal|否|该时间窗口的挂号费；未传时使用请求体中的 `defaultPrice`|
|room|String|否|该时间窗口优先使用的诊室；未传时从 `rooms` 列表中自动选择或为空|



**AiScheduleItem 字段说明**



|参数名|类型|必填|说明|
|---|---|---|---|
|doctorId|String|是|医生ID；发布时会作为排班所属医生写入 `doctor_schedule.doctor_id`|
|doctorName|String|是|医生姓名；用于展示和写入排班快照|
|deptId|String|是|科室ID；发布时会作为排班所属科室写入|
|workDate|Date|是|出诊日期，格式 `yyyy-MM-dd`|
|startTime|Time|是|出诊开始时间，格式 `HH:mm:ss`|
|endTime|Time|是|出诊结束时间，格式 `HH:mm:ss`；必须晚于 `startTime`|
|maxNum|Integer|是|本排班最大号源数；发布后通常会作为初始可预约号源数量|
|price|Decimal|是|本排班挂号费|
|room|String|否|出诊诊室|
|conflict|Boolean|否|冲突标记；`true` 表示该医生或诊室在同日期、同时间段已有冲突排班，或冲突检查失败|
|conflictType|String|否|冲突类型；常见值 `DOCTOR_TIME` 医生时间冲突、`ROOM_TIME` 诊室占用冲突、`BATCH_DOCTOR_TIME` 本批次医生冲突、`BATCH_ROOM_TIME` 本批次诊室冲突、`CHECK_FAILED`/`CHECK_EXCEPTION` 冲突检查失败|
|conflictReason|String|否|冲突原因；无冲突时通常为 `null` 或空字符串|



**AiScheduleGenerateResponse 字段说明**



|参数名|类型|说明|
|---|---|---|
|traceId|String|本次排班草稿追踪ID；当前预览实现可能返回 `null`，发布时可原样透传或留空|
|status|String|生成/检查状态；常见值：`SUCCESS` 表示成功，`FALLBACK` 表示模型不可用时使用规则降级生成|
|modelVersion|String|使用的大模型名称或版本；当前配置通常为 `deepseek-v4-flash`|
|summary|String|本次排班方案摘要，说明生成策略或冲突检查结果|
|items|AiScheduleItem\[\]|排班项列表；预览接口返回 AI 或规则生成的草稿，冲突检查接口返回带冲突标记的原排班项|
|warnings|String\[\]|警告信息列表；用于提示模型降级、读取已有排班失败、冲突检查异常、过滤无效排班等情况|
|optimizationReasons|String\[\]|AI 给出的排班优化理由，例如避开不可用日期、符合管理员偏好、均衡诊室使用等|
|fallback|Boolean|是否为降级结果；`true` 表示大模型调用失败，后端按规则生成了可供管理员继续检查的草稿|



**AiSchedulePublishResponse 字段说明**



|参数名|类型|说明|
|---|---|---|
|traceId|String|发布请求携带的排班草稿追踪ID；用于关联预览与发布流程|
|status|String|发布结果；`SUCCESS` 表示全部创建成功，`PARTIAL_SUCCESS` 表示部分创建成功，`FAILED` 表示没有可发布项或调用 admin\-service 创建失败|
|submittedCount|Integer|本次提交的排班项数量，包含冲突项和非冲突项|
|createdCount|Integer|实际成功创建的排班数量|
|createdSchedules|DoctorSchedule\[\]|admin\-service 返回的已创建排班记录列表|
|failedItems|SchedulePublishFailure\[\]|admin\-service 批量创建时未创建的排班项及失败原因；包含医生已停职、医生时间冲突、诊室占用冲突、本批次内部冲突等|
|warnings|String\[\]|发布警告或失败原因，例如存在冲突项被过滤、没有可发布排班、部分排班未创建等|



**SchedulePublishFailure 字段说明**



|参数名|类型|说明|
|---|---|---|
|index|Integer|失败项在发布请求 `items` 数组中的下标，从0开始|
|doctorId|String|医生ID|
|doctorName|String|医生姓名|
|workDate|Date|出诊日期|
|startTime|Time|出诊开始时间|
|endTime|Time|出诊结束时间|
|room|String|诊室|
|reason|String|失败原因|
|conflictType|String|冲突类型或失败类型；常见值同 `AiScheduleItem.conflictType`|



**DoctorSchedule 主要字段说明**



|参数名|类型|说明|
|---|---|---|
|scheduleId|String|排班ID，由 admin\-service 创建后返回|
|planId|String|排班计划ID；当前 AI 批量创建场景可能为空|
|doctorId|String|医生ID|
|doctorName|String|医生姓名|
|deptId|String|科室ID|
|workDate|Date|出诊日期|
|startTime|Time|出诊开始时间|
|endTime|Time|出诊结束时间|
|maxNum|Integer|最大号源数|
|remainNum|Integer|剩余号源数|
|status|Integer|排班启用状态；通常 `1` 表示启用，`0` 表示停用|
|price|Decimal|挂号费|
|room|String|诊室|
|sourceType|String|排班来源；AI 创建场景可能由 admin\-service 标记|
|scheduleStatus|String|排班业务状态；取值以 admin\-service 实际实现为准|
|createTime|DateTime|创建时间|
|updateTime|DateTime|更新时间|



##### 2\.4\.2\.1 AI排班预览



|项目|内容|
|---|---|
|接口地址|`/ai-service/schedule/preview`|
|请求方式|`POST`|
|请求头|`Authorization: Bearer {token}` 或 `token: 管理员JWT`|
|权限说明|管理员；Controller 会校验管理员角色|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|doctorId|body|String|是|医生ID|
|doctorName|body|String|是|医生姓名|
|deptId|body|String|是|科室ID|
|periodStart|body|Date|是|排班开始日期|
|periodEnd|body|Date|是|排班结束日期；周期最多31天|
|requirement|body|String|否|管理员自然语言要求|
|defaultMaxNum|body|Integer|否|默认号源数，1\-500，默认30|
|defaultPrice|body|Decimal|否|默认挂号费，不能小于0，默认0\.00|
|rooms|body|String\[\]|否|可用诊室列表|
|timeWindows|body|AiScheduleTimeWindow\[\]|否|可用时间窗口；为空时使用工作日默认上午/下午窗口；字段含义见上方 `AiScheduleTimeWindow 字段说明`|
|unavailableDates|body|Date\[\]|否|不可排班日期|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|AiScheduleGenerateResponse|AI生成的排班草稿和冲突检查结果；字段含义见上方 `AiScheduleGenerateResponse 字段说明`|
|data\.items|AiScheduleItem\[\]|可供管理员预览、调整、冲突检查和发布的排班项；字段含义见上方 `AiScheduleItem 字段说明`|



**请求示例**



```JSON
{
  "doctorId": "DOC001",
  "doctorName": "李医生",
  "deptId": "DEPT001",
  "periodStart": "2026-07-01",
  "periodEnd": "2026-07-07",
  "requirement": "优先安排周一、周三上午门诊，避开周五。",
  "defaultMaxNum": 30,
  "defaultPrice": 30,
  "rooms": [
    "101",
    "102"
  ],
  "timeWindows": [
    {
      "dayOfWeek": 1,
      "startTime": "09:00:00",
      "endTime": "12:00:00",
      "maxNum": 30,
      "price": 30,
      "room": "101"
    },
    {
      "dayOfWeek": 3,
      "startTime": "09:00:00",
      "endTime": "12:00:00",
      "maxNum": 30,
      "price": 30,
      "room": "102"
    }
  ],
  "unavailableDates": [
    "2026-07-03"
  ]
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "traceId": null,
    "status": "SUCCESS",
    "modelVersion": "deepseek-v4-flash",
    "summary": "根据管理员要求生成周一、周三上午门诊排班草稿。",
    "items": [
      {
        "doctorId": "DOC001",
        "doctorName": "李医生",
        "deptId": "DEPT001",
        "workDate": "2026-07-01",
        "startTime": "09:00:00",
        "endTime": "12:00:00",
        "maxNum": 30,
        "price": 30,
        "room": "101",
        "conflict": false,
        "conflictType": null,
        "conflictReason": null
      }
    ],
    "warnings": [],
    "optimizationReasons": [
      "符合管理员指定时间偏好",
      "已跳过不可排班日期"
    ],
    "fallback": false
  }
}
```



**错误码：** 400 参数校验失败；500/业务异常 管理员token无效、非管理员角色；200\+FALLBACK 表示模型不可用时按规则生成草稿



**业务规则：** 先校验管理员身份；再读取 admin\-service 中该医生在请求周期内的已有排班，并读取请求 `rooms` 在同周期内的已发布诊室占用作为模型生成上下文。AI生成结果会被规范化：医生、科室信息以请求参数为准；无效日期、不可排班日期、无效时间段会被过滤；最多保留100条排班项；最后调用 admin\-service 结构化冲突检查并标记 `conflict/conflictType/conflictReason`。冲突检查同时覆盖医生同日时间重叠和诊室同日时间重叠；例如其他医生已占用同一诊室时会返回 `ROOM_TIME`。模型失败时使用配置的时间窗口生成规则降级草稿。



##### 2\.4\.2\.2 AI排班冲突检查



|项目|内容|
|---|---|
|接口地址|`/ai-service/schedule/conflict-check`|
|请求方式|`POST`|
|请求头|`Authorization: Bearer {token}` 或 `token: 管理员JWT`|
|权限说明|管理员；Controller 会校验管理员角色|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|items|body|AiScheduleItem\[\]|是|待检查排班项，最多100项；字段含义见上方 `AiScheduleItem 字段说明`|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|AiScheduleGenerateResponse|带冲突标记的排班项列表；字段含义见上方 `AiScheduleGenerateResponse 字段说明`|
|data\.items|AiScheduleItem\[\]|冲突检查后的排班项；重点查看每项的 `conflict`、`conflictType` 和 `conflictReason`|



**请求示例**



```JSON
{
  "items": [
    {
      "doctorId": "DOC001",
      "doctorName": "李医生",
      "deptId": "DEPT001",
      "workDate": "2026-07-01",
      "startTime": "09:00:00",
      "endTime": "12:00:00",
      "maxNum": 30,
      "price": 30,
      "room": "101"
    }
  ]
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "traceId": null,
    "status": "SUCCESS",
    "modelVersion": "deepseek-v4-flash",
    "summary": "Conflict check completed.",
    "items": [
      {
        "doctorId": "DOC001",
        "doctorName": "李医生",
        "deptId": "DEPT001",
        "workDate": "2026-07-01",
        "startTime": "09:00:00",
        "endTime": "12:00:00",
        "maxNum": 30,
        "price": 30,
        "room": "101",
        "conflict": true,
        "conflictType": "ROOM_TIME",
        "conflictReason": "诊室 101 在该时段已被 王医生 占用"
      }
    ],
    "warnings": [],
    "optimizationReasons": [],
    "fallback": false
  }
}
```



**错误码：** 400 参数校验失败；500/业务异常 管理员token无效、非管理员角色；冲突检查依赖服务异常时仍返回200，但对应排班项会标记 `conflict=true`



**业务规则：** 仅做冲突检查，不创建排班。每个排班项会通过 admin\-service 校验是否与已有已发布且启用的排班冲突；校验维度包括同一医生同日期时间重叠、同一诊室同日期时间重叠。校验失败或异常时按冲突处理，并在 `warnings` 中记录原因。



##### 2\.4\.2\.3 AI排班发布



|项目|内容|
|---|---|
|接口地址|`/ai-service/schedule/publish`|
|请求方式|`POST`|
|请求头|`Authorization: Bearer {token}` 或 `token: 管理员JWT`|
|权限说明|管理员；Controller 会校验管理员角色|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|traceId|body|String|否|排班草稿追踪ID|
|items|body|AiScheduleItem\[\]|是|待发布排班项，最多100项；字段含义见上方 `AiScheduleItem 字段说明`|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|AiSchedulePublishResponse|发布结果；字段含义见上方 `AiSchedulePublishResponse 字段说明`|
|data\.createdSchedules|DoctorSchedule\[\]|实际创建成功的排班记录；字段含义见上方 `DoctorSchedule 主要字段说明`|



**请求示例**



```JSON
{
  "traceId": "SCH202607010001",
  "items": [
    {
      "doctorId": "DOC001",
      "doctorName": "李医生",
      "deptId": "DEPT001",
      "workDate": "2026-07-01",
      "startTime": "09:00:00",
      "endTime": "12:00:00",
      "maxNum": 30,
      "price": 30,
      "room": "101",
      "conflict": false
    }
  ]
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "traceId": null,
    "status": "SUCCESS",
    "submittedCount": 1,
    "createdCount": 1,
    "createdSchedules": [
      {
        "scheduleId": "SCH001",
        "doctorId": "DOC001",
        "doctorName": "李医生",
        "deptId": "DEPT001",
        "workDate": "2026-07-01",
        "startTime": "09:00:00",
        "endTime": "12:00:00",
        "maxNum": 30,
        "price": 30,
        "room": "101"
      }
    ],
    "failedItems": [],
    "warnings": []
  }
}
```



**错误码：** 400 参数校验失败；500/业务异常 管理员token无效、非管理员角色；200\+FAILED/200\+PARTIAL\_SUCCESS 表示无可发布排班、admin\-service创建失败或部分排班未创建



**业务规则：** 发布前会再次执行结构化冲突检查；存在医生时间冲突、诊室时间冲突或检查失败的排班项会被过滤，不会提交给 admin\-service。无可发布排班时返回 `status=FAILED`。可发布排班通过 admin\-service 批量创建；admin\-service 落库前仍会再次校验医生时间冲突、诊室时间冲突，并检查本批次内部的医生/诊室时间重叠。全部成功返回 `SUCCESS`，部分创建返回 `PARTIAL_SUCCESS`，失败项通过 `failedItems` 返回原因。



#### 2\.4\.3 AI报告分析



##### 2\.4\.3\.1 AI报告分析



|项目|内容|
|---|---|
|接口地址|`/ai-service/report/analyze`|
|请求方式|`POST`|
|请求头|token: 医生JWT（必填）|
|权限说明|医生本人；通过内部接诊上下文校验医生与挂号记录归属|



**请求参数**



|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|registerId|body|String|是|挂号ID|
|reportType|body|String|否|报告类型；普通检查/检验可传 EXAM/LAB，CT 结构化结果建议传 CT_ARTIFACT_REPORT 或 CT_LESION_REPORT|
|reportText|body|String|条件必填|报告原文；与 indicators、reportInput 至少提供一项|
|indicators|body|IndicatorDto\[\]|条件必填|检验指标明细；与 reportText、reportInput 至少提供一项|
|reportInput|body|Map\<String,Object\>|条件必填|影像 AI 标准化 JSON 结果；可直接传 CT 金属伪影识别或 CT 病灶识别分割返回的 `data.reportInput`|



**返回参数**



|参数名|类型|说明|
|---|---|---|
|data|ReportAnalysisVo|AI报告分析结果|



**请求示例：普通检验报告**



```JSON
{
  "registerId": "REG001",
  "reportType": "LAB",
  "reportText": "脑脊液蛋白 0.62 g/L，参考范围 0.15-0.45 g/L"
}
```



**请求示例：CT病灶结构化结果**



```JSON
{
  "registerId": "REG001",
  "reportType": "CT_LESION_REPORT",
  "reportInput": {
    "task": "CT_LESION_REPORT",
    "modality": "CT",
    "finding": {
      "lesionDetected": true,
      "lesionPixels": 3264,
      "totalPixels": 31457280,
      "lesionRatio": 0.0104,
      "maskFile": "xxx_scan_lesion_mask.nii.gz",
      "lesionSliceIndices": [36, 37, 38],
      "lesionCount": 2,
      "largestLesionPixels": 2140,
      "fallback": false,
      "previewSliceIndex": 37,
      "previewImageFile": "xxx_scan_lesion_preview_z37.png",
      "previewImageUrl": "/previews/xxx_scan_lesion_preview_z37.png"
    },
    "model": {
      "modelType": "attention",
      "modelVersion": "lesion_attention_adamw_20260704_001918"
    },
    "summary": "检测到CT病灶候选区2处，候选像素占比约0.0104%。"
  }
}
```



**返回示例**



```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "summary": "CT病灶候选区需结合原始影像、临床症状和既往检查综合判断。",
    "riskLevel": "MEDIUM",
    "abnormalIndicators": [],
    "suggestions": [
      "由检查/检验医生结合原始CT影像复核AI分割区域",
      "必要时完善增强CT或相关实验室检查"
    ],
    "followUpAdvice": "建议结合病史和体征决定复查或进一步检查计划。",
    "fallback": false
  }
}
```



**错误码：** 400 参数校验失败；500 Token无效、非医生角色、挂号ID为空、无法获取患者接诊信息或无权访问；200\+fallback=true 表示大模型调用失败后返回规则降级分析。



**业务规则：** 普通检查/检验报告分析调用前通过内部接口校验医生拥有该接诊记录。CT 金属伪影识别和 CT 病灶识别分割产生的结构化 `reportInput` 可由检查医生工作流提交，`reportType` 传 `CT_ARTIFACT_REPORT` 或 `CT_LESION_REPORT` 时不再强制要求当前医生是该挂号的接诊医生。`reportText`、`indicators`、`reportInput` 至少提供一项；其中 `reportInput` 用于接收影像 AI 输出的标准化 JSON。服务会把报告文本、结构化检验指标或影像 AI 结构化结果交给大模型分析，输出仅供医生参考，不替代原始影像复核或正式诊断。



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
|CT伪影检测预览图下载|`GET`|`/admin-service/ml/inference/ct-artifact/preview/{previewFilename}`|
|CT病灶识别与分割推理|`POST`|`/admin-service/ml/inference/ct-lesion`|
|CT病灶识别与分割结果掩膜下载|`GET`|`/admin-service/ml/inference/ct-lesion/result/{maskFilename}`|
|CT病灶识别与分割预览图下载|`GET`|`/admin-service/ml/inference/ct-lesion/preview/{previewFilename}`|

> **四角色拆分（CT 归属检查医生）**：CT 伪影检测虽物理上仍由 `ai-service` 的 `MlOpsController` 实现，但业务上归**检查医生（roleType=2, doctorType=2）**，不再属于管理员 MLOps。前端检查医生页面应调用 doctor-service 语义路径，由 doctor-service 完成医生身份校验后代理到 ai-service：
>
> | 检查医生调用路径 | doctor-service 代理目标 |
> |---|---|
> | `POST /doctor-service/exam/ct-artifact` | `POST /admin-service/ml/inference/ct-artifact` |
> | `GET /doctor-service/exam/ct-artifact/result/{maskFilename}` | `GET /admin-service/ml/inference/ct-artifact/result/{maskFilename}` |
> | `GET /doctor-service/exam/ct-artifact/preview/{previewFilename}` | `GET /admin-service/ml/inference/ct-artifact/preview/{previewFilename}` |
> | `POST /doctor-service/exam/ct-lesion` | `POST /admin-service/ml/inference/ct-lesion` |
> | `GET /doctor-service/exam/ct-lesion/result/{maskFilename}` | `GET /admin-service/ml/inference/ct-lesion/result/{maskFilename}` |
> | `GET /doctor-service/exam/ct-lesion/preview/{previewFilename}` | `GET /admin-service/ml/inference/ct-lesion/preview/{previewFilename}` |
>
> `gateway-server/application-route.yml` 和 Vite 开发代理不再对上述 doctor-service 路径做特殊重写，避免绕过检查医生权限校验。原 `/admin-service/ml/inference/ct-artifact`、`/admin-service/ml/inference/ct-lesion` 路径仍可用于管理员/MLOps 兼容访问；检查医生端前端必须优先调用 doctor-service 语义路径。



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
|artifactSliceIndices|Integer\[\]|存在伪影阳性像素/体素的 Z 轴切片索引|
|previewSliceIndex|Integer|用于前端预览的 Z 轴切片索引；优先选择伪影像素最多的切片|
|previewImageFile|String|生成的 2D 预览 PNG 文件名|
|previewImageUrl|String|2D 预览 PNG 下载路径；前端应通过 Java 代理下载|
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
|finding|Object|artifactDetected、positivePixels、totalPixels、artifactRatio、maskFile、artifactSliceIndices、previewSliceIndex、previewImageFile、previewImageUrl|
|imageMeta|Object|shape、spacing、origin|
|model|Object|modelType、modelVersion|
|summary|String|可直接交给大语言模型参考的摘要|

##### `CtLesionInferenceResult`

|字段|类型|说明|
|---|---|---|
|status|String|Python 推理状态，成功为 success|
|message|String|推理结果提示|
|originalFile|String|原始上传文件名|
|maskFile|String|生成的病灶候选区掩膜文件名|
|shape|Integer\[\]|影像维度|
|spacing|Double\[\]|像素间距|
|origin|Double\[\]|影像原点|
|downloadUrl|String|掩膜下载路径；前端应通过 Java 代理下载|
|lesionSliceIndices|Integer\[\]|存在病灶候选像素/体素的 Z 轴切片索引|
|previewSliceIndex|Integer|用于前端预览的 Z 轴切片索引；优先选择病灶候选像素最多的切片|
|previewImageFile|String|生成的 2D 预览 PNG 文件名|
|previewImageUrl|String|2D 预览 PNG 下载路径；前端应通过 Java 代理下载|
|lesionDetected|Boolean|是否检测到病灶候选区|
|lesionPixels|Integer|病灶候选阳性像素/体素数|
|totalPixels|Integer|总像素/体素数|
|lesionRatio|Double|病灶候选区占比，百分数|
|lesionCount|Integer|按连通域统计的病灶候选区数量|
|largestLesionPixels|Integer|最大病灶候选连通域像素/体素数|
|fallback|Boolean|是否为降级结果；`false` 表示已使用训练权重推理，`true` 表示权重缺失或未配置时的启发式候选结果|
|modelType|String|模型结构，当前默认 attention|
|modelVersion|String|模型版本，当前部署为 lesion_attention_adamw_20260704_001918；未加载病灶权重时为 heuristic_no_weights|
|summary|String|结构化摘要|
|reportInput|CtLesionReportInput|给后续大语言模型生成文字描述/报告初稿的结构化输入|
|logId|String|Java 侧推理日志ID|
|latencyMs|Long|Java 调用 Python 总耗时毫秒|

##### `CtLesionReportInput`

|字段|类型|说明|
|---|---|---|
|task|String|固定为 CT_LESION_REPORT|
|modality|String|固定为 CT|
|finding|Object|lesionDetected、lesionPixels、totalPixels、lesionRatio、maskFile、lesionSliceIndices、lesionCount、largestLesionPixels、fallback、previewSliceIndex、previewImageFile、previewImageUrl|
|imageMeta|Object|shape、spacing、origin|
|model|Object|modelType、modelVersion|
|summary|String|可直接交给大语言模型参考的摘要；LLM 不负责重新判断病灶区域|



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
    "artifactSliceIndices": [42, 43, 44],
    "previewSliceIndex": 43,
    "previewImageFile": "xxx_scan_preview_z43.png",
    "previewImageUrl": "/previews/xxx_scan_preview_z43.png",
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
        "maskFile": "xxx_scan_mask.nii.gz",
        "artifactSliceIndices": [42, 43, 44],
        "previewSliceIndex": 43,
        "previewImageFile": "xxx_scan_preview_z43.png",
        "previewImageUrl": "/previews/xxx_scan_preview_z43.png"
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

**业务规则：** Python 服务生成 3D 掩膜、结构化统计和 2D PNG 预览图。预览图为选定 Z 轴 CT 灰度切片叠加红色伪影候选区，用于前端优先展示；完整 3D 掩膜仍通过下载接口保留。Java 后端追加 `logId`、`latencyMs` 并记录推理日志。`reportInput` 是后续大语言模型生成文字描述和报告初稿的输入，LLM 不负责重新判断伪影区域。

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



##### 2\.5\.1\.12 CT伪影检测预览图下载

|项目|内容|
|---|---|
|接口地址|`/doctor-service/exam/ct-artifact/preview/{previewFilename}`（检查医生语义路径）|
|兼容地址|`/admin-service/ml/inference/ct-artifact/preview/{previewFilename}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|业务上归检查医生；当前 Controller 未单独校验 doctorType|

**请求参数**

|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|previewFilename|path|String|是|推理返回的预览 PNG 文件名，不允许包含 `/` 或 `\`|

**返回参数**

二进制 PNG 图片，`Content-Type: image/png`。图片内容为 CT 灰度切片叠加红色金属伪影候选区。

**请求示例**

```HTTP
GET /doctor-service/exam/ct-artifact/preview/xxx_scan_preview_z43.png
无请求体
```

**业务规则：** 前端通过 Java 后端代理下载预览图，不直接暴露 Python 服务地址。该图只展示模型分割结果，医生仍需复核确认。



##### 2\.5\.1\.13 CT病灶识别与分割推理
|项目|内容|
|---|---|
|接口地址|`/doctor-service/exam/ct-lesion`（检查医生语义路径）|
|兼容地址|`/admin-service/ml/inference/ct-lesion`|
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
|data|CtLesionInferenceResult|CT 病灶识别与分割结构化结果|

**请求示例**

```HTTP
POST /doctor-service/exam/ct-lesion
multipart/form-data: file=<scan.nii.gz>
```

**返回示例**

```JSON
{
  "code": 200,
  "msg": "成功",
  "data": {
    "status": "success",
    "message": "CT病灶识别与分割完成",
    "originalFile": "scan.nii.gz",
    "maskFile": "xxx_scan_lesion_mask.nii.gz",
    "lesionSliceIndices": [36, 37, 38],
    "previewSliceIndex": 37,
    "previewImageFile": "xxx_scan_lesion_preview_z37.png",
    "previewImageUrl": "/previews/xxx_scan_lesion_preview_z37.png",
    "lesionDetected": true,
    "lesionPixels": 3264,
    "totalPixels": 31457280,
    "lesionRatio": 0.0104,
    "lesionCount": 2,
    "largestLesionPixels": 2140,
    "fallback": false,
    "modelType": "attention",
    "modelVersion": "lesion_attention_adamw_20260704_001918",
    "summary": "检测到CT病灶候选区2处，候选像素占比约0.0104%。",
    "reportInput": {
      "task": "CT_LESION_REPORT",
      "modality": "CT",
      "finding": {
        "lesionDetected": true,
        "lesionPixels": 3264,
        "totalPixels": 31457280,
        "lesionRatio": 0.0104,
        "maskFile": "xxx_scan_lesion_mask.nii.gz",
        "lesionSliceIndices": [36, 37, 38],
        "lesionCount": 2,
        "largestLesionPixels": 2140,
        "fallback": false,
        "previewSliceIndex": 37,
        "previewImageFile": "xxx_scan_lesion_preview_z37.png",
        "previewImageUrl": "/previews/xxx_scan_lesion_preview_z37.png"
      },
      "imageMeta": {
        "shape": [120, 512, 512],
        "spacing": [1.0, 0.8, 0.8],
        "origin": [0.0, 0.0, 0.0]
      },
      "model": {
        "modelType": "attention",
        "modelVersion": "lesion_attention_adamw_20260704_001918"
      },
      "summary": "检测到CT病灶候选区2处，候选像素占比约0.0104%。"
    },
    "logId": "INF0123456789abcdef",
    "latencyMs": 910
  }
}
```

**错误码：** 400 文件格式不支持；500 Python 服务不可用或推理异常。

**业务规则：** Python 服务生成 3D 病灶候选掩膜、结构化统计和 2D PNG 预览图。预览图为选定 Z 轴 CT 灰度切片叠加红色病灶候选区，用于前端优先展示；完整 3D 掩膜仍通过下载接口保留。当前已部署训练权重 `Model/weights/best_lesion_attention.pth`，默认模型版本为 `lesion_attention_adamw_20260704_001918`；仅当 `LESION_MODEL_PATH` 未配置或权重不存在时，Python 服务返回 `fallback=true` 且 `modelVersion=heuristic_no_weights`，表示服务链路可运行的启发式候选结果，不代表训练模型诊断。`reportInput` 是后续大语言模型生成文字描述和报告初稿的标准化 JSON 输入，可直接提交给 `/ai-service/report/analyze`；LLM 不负责重新判断病灶区域。

##### 2\.5\.1\.14 CT病灶识别与分割结果掩膜下载
|项目|内容|
|---|---|
|接口地址|`/doctor-service/exam/ct-lesion/result/{maskFilename}`（检查医生语义路径）|
|兼容地址|`/admin-service/ml/inference/ct-lesion/result/{maskFilename}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|业务上归检查医生；当前 Controller 未单独校验 doctorType|

**请求参数**

|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|maskFilename|path|String|是|推理返回的掩膜文件名，不允许包含 `/` 或 `\`|

**返回参数**

二进制 NIfTI 掩膜文件，`Content-Type: application/octet-stream`。

##### 2\.5\.1\.15 CT病灶识别与分割预览图下载
|项目|内容|
|---|---|
|接口地址|`/doctor-service/exam/ct-lesion/preview/{previewFilename}`（检查医生语义路径）|
|兼容地址|`/admin-service/ml/inference/ct-lesion/preview/{previewFilename}`|
|请求方式|`GET`|
|请求头|JSON|
|权限说明|业务上归检查医生；当前 Controller 未单独校验 doctorType|

**请求参数**

|参数名|位置|类型|必填|说明|
|---|---|---|---|---|
|previewFilename|path|String|是|推理返回的预览 PNG 文件名，不允许包含 `/` 或 `\`|

**返回参数**

二进制 PNG 图片，`Content-Type: image/png`。图片内容为 CT 灰度切片叠加红色病灶候选区。

### 2\.6 支付模块



**模块职责：** 提供统一支付订单创建、支付状态变更、业务支付状态联动和患者支付历史查询。



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



**业务规则：** 网关已配置 `/api/payment/**` 到 payment-service 的重写路由；服务内路径仍为 `/payment-service/pay/**`，服务间 Feign 调用继续使用该内部路径。



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

2. `/api/payment/**` 网关路由已配置；后续若新增支付接口，需要保持网关重写与 Controller 路径同步。

3. 将患者病历/处方 `my-list` 接口改为从 token 读取 patientId，禁止客户端任意指定。

4. 为接诊详情、草稿、检查单和完成接诊补充记录归属校验。

5. 为 `/api/admin/**` 补充管理员角色校验，并同步修正前端 MLOps 请求路径。

6. 完成头像文件持久化、短信验证码验证和医生资料相关 TODO 后，再补充对应正式接口。

7. 统一 BusinessException 的业务 code 与 HTTP 状态，避免所有业务错误都表现为 code=500。

## 4. 2026-07-02 医技检查/检验支付与分配补充

### 4.1 开具检查/检验申请

- 接诊医生主流程通过 `/doctor-service/consult/medical-order/confirm` 确认检查/检验申请时，后端写入 `medical_order`、`medical_order_item`，并尝试调用 `payment-service/pay/create` 创建 `orderType=MEDICAL` 的支付订单；当前开发联调流不以 payment-service 成功作为接诊阻塞条件。
- 兼容旧接口 `/doctor-service/consult/create-exam-order` 保留，但接诊详情页不再作为主入口使用。
- 检查/检验项目必须能在 `medical_item` 字典中匹配；未知项目会拒绝开单，不再以 0 元项目写入或创建支付订单。
- 支付订单 `businessId` 等于 `medical_order.order_id`，初始 `payStatus=WAITING`。
- 新主流程会在保存医技申请后将 `medical_order.pay_status` 推进到 `PAID`，并将主单与明细状态推进到 `QUEUED`，确保检查/检验医生队列能立即读取任务；支付服务不可用时只返回非阻断提示。

### 4.2 支付状态联动

- `POST /payment-service/pay/success/{payId}` 支付成功后，先更新 `pay.pay_status=PAID`。`PRESCRIPTION` 更新 `prescription.pay_status=PAID`，`REGISTER` 更新 `registration.pay_status=PAID`。
- `MEDICAL` 支付成功后，payment-service 不再直接更新 `medical_order`，而是调用 doctor-service 内部回调 `/doctor-service/internal/medical-order/payment-success/{orderId}`，由 doctor-service 统一将 `medical_order.pay_status` 推进到 `PAID`、将 `medical_order.status` 从 `WAITING_ASSIGN` 推进到 `QUEUED`，并将该申请下仍为 `WAITING_ASSIGN` 的 `medical_order_item.status` 推进到 `QUEUED`。该回调按幂等处理：如果支付状态已是 `PAID`，doctor-service 仍会继续执行入队；这样避免 payment-service 未提交事务占用 `medical_order` 行锁导致跨服务回调超时。
- 医生端 CT 推理上传经 `/doctor-service/exam/ct-artifact` 和 `/doctor-service/exam/ct-lesion` 代理到 ai-service；doctor-service 与 ai-service 均配置 multipart 上限 `100MB`，避免浏览器路径先被 doctor-service 拦截。
- 取消和退款分别同步为 `CANCELLED`、`REFUNDED`；如果业务表未更新到对应记录，本次支付状态变更失败并回滚。

### 4.3 检查/检验医生申请列表与分配

| 接口名称 | 请求方式 | 接口地址 |
|---|---|---|
| 查询检查/检验申请列表 | `GET` | `/inspection-doctor/orders` |
| 兼容旧地址 | `GET` | `/inspection-doctor/lab-orders` |
| 分配检查/检验申请 | `POST` | `/inspection-doctor/order/{orderId}/assign` |
| 查询当前医生可领取队列 | `GET` | `/doctor-service/task/queue` |
| 检查/检验医生提交报告 | `POST` | `/doctor-service/task/report` |

`/inspection-doctor/orders`、`/inspection-doctor/order/{orderId}` 及 `/inspection-doctor/order/{orderId}/assign` 均为后端兼容接口，当前前端已无入口（旧检验申请列表页 `InspectionOrderList.vue` 已删除，`/inspection-doctor/order-list` 重定向到 `/inspection-doctor/queue`）。检查医生和检验医生主流程统一复用 `/doctor-service/task/queue` 与 `/doctor-service/task/workbench`。

分配请求体：

```JSON
{
  "assignedRoom": "CT-1"
}
```

分配业务规则：

- 仅检查/检验医生可调用。
- 只有 `pay_status=PAID` 且 `status=WAITING_ASSIGN` 的申请可以分配。
- `assignedRoom` 必须非空；分配成功后写入 `medical_order.assigned_room`，并将 `medical_order.status` 更新为 `QUEUED`。
- `/doctor-service/task/queue` 仅返回当前医生类型可领取的已支付排队明细：检查医生（`doctorType=2`）只返回 `item_category=EXAM`，检验医生（`doctorType=3`）只返回 `item_category=LAB`；`queueCount` 与 `tasks` 使用同一过滤口径。
- 检查/检验医生工作台展示当前医生自己的任务；可领取队列展示尚未领取的 `QUEUED` 任务，两者语义分开。

报告提交请求体：

```JSON
{
  "orderItemId": "MOI001",
  "resultSummary": "颅脑CT平扫未见明显异常密度影。",
  "conclusion": "颅脑CT平扫未见明显异常。",
  "abnormalFlag": "NORMAL",
  "attachmentUrl": "/files/reports/ct/MR001.zip"
}
```

报告提交业务规则：

- 仅检查/检验医生可调用。
- 任务必须为当前医生领取中的 `IN_PROCESS` 状态。
- 提交后写入或覆盖 `medical_report`，状态为 `PUBLISHED`，并同步将对应 `medical_order_item` 标记为 `COMPLETED`。
- 接诊医生通过 `/doctor-service/consult/reports?registerId=...` 查询本次已发布报告；接诊列表通过 `reportCount/latestReportTime/hasReturnedReport` 提示“报告已回传/待复诊分析”。
