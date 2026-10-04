(()=>{"use strict";
// Configure window.API_BASE at deployment only when the frontend and API use different origins.
const API=(window.API_BASE||"").replace(/\/$/,"");
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const P={eye:'<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',off:'<path d="M3 3l18 18M10.6 6.1A10 10 0 0 1 12 6c6.5 0 10 6 10 6a17 17 0 0 1-3.2 3.9M6.5 7A16.6 16.6 0 0 0 2 12s3.5 6 10 6c1.5 0 2.8-.3 4-.8"/>',lock:'<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>',shield:'<path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/><path d="M9 12l2 2 4-4"/>',left:'<path d="M19 12H5M11 6l-6 6 6 6"/>',right:'<path d="M5 12h14M13 6l6 6-6 6"/>',bag:'<path d="M6 8h12l1 12H5z"/><path d="M9 8a3 3 0 0 1 6 0"/>',store:'<path d="M4 9v11h16V9M3 9l1.5-5h15L21 9c0 1.7-1.3 3-3 3s-3-1.3-3-3c0 1.7-1.3 3-3 3S9 10.7 9 9c0 1.7-1.3 3-3 3S3 10.7 3 9zM10 20v-5h4v5"/>',pin:'<path d="M12 21s7-6 7-11a7 7 0 0 0-14 0c0 5 7 11 7 11z"/><circle cx="12" cy="10" r="2.5"/>',list:'<path d="M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01"/>',up:'<path d="M12 16V4M7 9l5-5 5 5M4 20h16"/>'};
const svg=n=>`<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${P[n]}</svg>`;
$$("[data-i]").forEach(e=>e.innerHTML=svg(e.dataset.i));

const V={required:v=>v.trim()?"":"Thông tin bắt buộc",
email:v=>!v||/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v.trim())?"":"Email không hợp lệ",
phone:v=>!v||/^(0|\+84)\d{9}$/.test(v.replace(/[\s.-]/g,""))?"":"Số điện thoại không hợp lệ",
otp:v=>/^\d{6}$/.test(v)?"":"Nhập mã OTP gồm sáu chữ số.",
username:v=>!v||/^[A-Za-z0-9_.]{4,30}$/.test(v)?"":"Tên đăng nhập gồm 4–30 ký tự: chữ, số, _ hoặc .",
min8:v=>!v||v.length>=8?"":"Mật khẩu phải có ít nhất 8 ký tự"};
const check=el=>{let m="";
 for(const r of (el.dataset.v||"").split(" ").filter(Boolean)){const[k,p]=r.split(":");
  if(k==="required")m=el.type==="checkbox"?(el.checked?"":el.dataset.requiredMessage||"Vui lòng xác nhận để tiếp tục"):V.required(el.value);
  else if(k==="match")m=el.value===el.form.elements[p].value?"":"Mật khẩu xác nhận không khớp";
  else m=V[k](el.value);
  if(m)break}
 const box=el.closest(".field"),e=$(".err",box);if(!e.id)e.id="e"+Math.random().toString(36).slice(2,8);
 box.classList.toggle("invalid",!!m);e.textContent=m;
 el.setAttribute("aria-invalid",String(!!m));el.setAttribute("aria-describedby",e.id);return !m};
const validate=root=>{let ok=$$("[data-v]",root).map(check).every(Boolean);
 for(const group of $$("[data-address-group]",root)){const inputs=$$("input",group),started=inputs.some(el=>el.value.trim());
  if(started)for(const el of inputs){if(!el.value.trim()){const box=el.closest(".field"),err=$(".err",box);err.textContent=el.dataset.requiredMessage||"Thông tin bắt buộc";box.classList.add("invalid");el.setAttribute("aria-invalid","true");ok=false}else check(el)}}
 if(!ok){const b=$(".invalid input,.invalid textarea",root);b&&b.focus()}return ok};
document.addEventListener("focusout",e=>e.target.dataset&&e.target.dataset.v&&check(e.target));
document.addEventListener("input",e=>e.target.closest&&e.target.closest(".invalid")&&check(e.target));
document.addEventListener("change",e=>{if(e.target.type==="file"){const n=$(".fname",e.target.closest(".drop"));n.textContent=e.target.files[0]?e.target.files[0].name:"Chưa chọn tệp"}});
document.addEventListener("click",e=>{
 const b=e.target.closest(".eye");
 if(b){const i=$("input",b.parentNode),show=i.type==="password";i.type=show?"text":"password";
  b.setAttribute("aria-label",show?"Ẩn mật khẩu":"Hiện mật khẩu");b.innerHTML=svg(show?"off":"eye")}
});

const collect=f=>{const o={};$$("[name]",f).forEach(el=>{
 if(el.type==="file"||el.dataset.skip!==undefined)return;
 const v=el.type==="checkbox"?el.checked:el.type==="password"?el.value:el.value.trim();
 if(v===""&&!(el.dataset.v||"").includes("required"))return;
 el.name.split(".").reduce((a,k,i,r)=>i===r.length-1?(a[k]=v):(a[k]=a[k]||{}),o)});return o};
const maskEmail=value=>{const [name,domain]=value.split("@");if(!domain)return value;const visible=name.slice(0,1);return `${visible}${"*".repeat(Math.min(6,Math.max(3,name.length-1)))}@${domain}`};
let otpCountdownTimer;
const startOtpCountdown=form=>{
 const button=$("[data-otp-resend]",form),status=$("[data-otp-countdown]",form);
 if(!button||!status)return;
 clearInterval(otpCountdownTimer);
 let seconds=Number(form.dataset.otpCooldown||60);
 button.disabled=true;
 const update=()=>{status.textContent=seconds>0?`Bạn có thể gửi lại mã sau ${seconds} giây.`:"Bạn có thể gửi lại mã.";if(seconds<=0){button.disabled=false;clearInterval(otpCountdownTimer)}seconds--};
 update();otpCountdownTimer=setInterval(update,1000)
};
const apiError=(body,status)=>{
 const messages={
  OTP_INVALID:"Mã OTP không chính xác hoặc đã được sử dụng. Hãy kiểm tra và thử lại.",
  OTP_EXPIRED:"Mã OTP đã hết hạn. Hãy gửi mã mới.",
  OTP_MAX_ATTEMPTS:"Bạn đã nhập sai quá số lần cho phép. Hãy gửi mã mới.",
  OTP_COOLDOWN:"Bạn vừa yêu cầu mã. Vui lòng chờ hết thời gian đếm ngược để gửi lại.",
  OTP_RATE_LIMITED:"Bạn đã yêu cầu quá nhiều mã. Vui lòng thử lại sau.",
  EMAIL_DELIVERY_UNAVAILABLE:"Email OTP chưa sẵn sàng. Vui lòng thử lại sau hoặc liên hệ hỗ trợ.",
  EMAIL_DELIVERY_FAILED:"Không gửi được email OTP qua Gmail SMTP. Vui lòng thử lại sau.",
  OTP_CONFIGURATION_UNAVAILABLE:"Email OTP chưa được cấu hình đầy đủ trên máy chủ."
 };
 return new Error(messages[body.code]||body.message||({400:"Dữ liệu không hợp lệ.",429:"Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.",503:"Dịch vụ hiện chưa khả dụng. Vui lòng thử lại sau."}[status]||"Không thể hoàn tất yêu cầu. Vui lòng thử lại sau."))
};
document.addEventListener("click",async e=>{
 const button=e.target.closest("[data-otp-resend]");if(!button||button.disabled)return;
 const form=button.closest("form"),msg=$(".form-msg",form);
 msg.className="form-msg";msg.textContent="";button.setAttribute("aria-busy","true");
 const originalText=button.textContent;button.textContent="Đang gửi mã…";
 try{
  const response=await fetch(API+"/api/auth/otp/resend",{method:"POST",credentials:"include",headers:{"Content-Type":"application/json","X-Requested-With":"fetch"},body:JSON.stringify(collect(form))});
  const body=await response.json().catch(()=>({}));
  if(!response.ok)throw apiError(body,response.status);
  msg.textContent=body.message||"Nếu email tồn tại, mã OTP sẽ được gửi.";msg.classList.add("show","success");
  startOtpCountdown(form)
 }catch(error){
  msg.textContent=error instanceof TypeError?"Không thể kết nối máy chủ. Vui lòng thử lại sau.":error.message;
  msg.classList.add("show");
  if(error.message.includes("chờ hết thời gian"))startOtpCountdown(form)
 }finally{button.removeAttribute("aria-busy");button.textContent=originalText}
});

const wiz=$("form[data-wizard]");
if(wiz){const S=$$("[data-step]",wiz),T=$$("#steps li"),n=S.length;let i=0;
 const L=[["owner.fullName","Chủ cửa hàng"],["owner.email","Email"],["owner.phone","Số điện thoại"],["owner.username","Tên đăng nhập"],["store.name","Cửa hàng"],["store.phone","SĐT cửa hàng"]];
 const summary=()=>{const d=$("#sum"),g=k=>wiz.elements[k].value.trim();d.replaceChildren();
  [...L.map(([k,l])=>[l,g(k)]),["Địa chỉ",["store.detailedAddress","store.ward","store.district","store.province"].map(g).filter(Boolean).join(", ")]]
  .forEach(([a,b])=>{const t=document.createElement("dt"),u=document.createElement("dd");t.textContent=a;u.textContent=b||"—";d.append(t,u)})};
 const show=k=>{i=k;S.forEach((s,j)=>s.classList.toggle("on",j===k));
  T.forEach((t,j)=>{t.classList.toggle("on",j===k);t.classList.toggle("done",j<k);j===k?t.setAttribute("aria-current","step"):t.removeAttribute("aria-current")});
  $("[data-prev]").hidden=k===0;$("[data-next]").hidden=k===n-1;$("[type=submit]",wiz).hidden=k!==n-1;
  if(k===n-1)summary();scrollTo({top:0,behavior:"smooth"})};
 const go=()=>{if(validate(S[i]))show(i+1)};
 wiz.addEventListener("click",e=>{if(e.target.closest("[data-prev]"))show(i-1);if(e.target.closest("[data-next]"))go()});
 wiz.addEventListener("submit",e=>{if(i<n-1){e.preventDefault();e.stopImmediatePropagation();go()}});
 show(0)}

document.addEventListener("submit",async e=>{const f=e.target;if(!f.dataset.endpoint)return;e.preventDefault();
 const msg=$(".form-msg",f),btn=$("[type=submit]",f);msg.className="form-msg";msg.textContent="";
 if(!validate(f))return;btn.setAttribute("aria-busy","true");
 const originalText=btn.textContent;
 btn.disabled=true;
 btn.textContent=f.dataset.otpSend?"Đang gửi mã…":f.dataset.otpVerify?"Đang xác minh…":"Đang xử lý…";
 if(f.dataset.endpoint==="/api/auth/reset-password"&&!$("[name=token]",f).value){msg.textContent="Mã xác minh bị thiếu hoặc không hợp lệ. Hãy yêu cầu OTP mới.";msg.classList.add("show");btn.removeAttribute("aria-busy");btn.disabled=false;btn.textContent=originalText;return}
 if(f.dataset.otpSend&&f.dataset.otpSent)f.dataset.endpoint="/api/auth/otp/resend";
 try{const r=await fetch(API+f.dataset.endpoint,{method:"POST",credentials:"include",headers:{"Content-Type":"application/json","X-Requested-With":"fetch"},body:JSON.stringify(collect(f))});
  const d=await r.json().catch(()=>({}));
  if(!r.ok){if(d.code==="OTP_COOLDOWN")startOtpCountdown(f);throw apiError(d,r.status)}
  if(f.dataset.otpSend){
   f.dataset.otpSent="true";
   const followup=f.dataset.otpFollowup?$(f.dataset.otpFollowup):null;
   if(followup){
    for(const key of ["channel","purpose","destination"])if(followup.elements[key])followup.elements[key].value=f.elements[key].value;
    const masked=$(f.dataset.otpMask);if(masked)masked.textContent=maskEmail(f.elements.destination.value);
    followup.hidden=false;startOtpCountdown(followup)
   }
   msg.textContent=d.message||"Nếu email tồn tại, mã OTP sẽ được gửi.";msg.classList.add("show","success");
   if(f.dataset.otpFollowup)f.hidden=true;
   return
  }
  if(f.dataset.otpVerify){
   const result=d.data||{};
   if(result.resetToken){sessionStorage.setItem("passwordResetToken",result.resetToken);location.href=f.dataset.resetPage;return}
   sessionStorage.removeItem("otpEmailDestination");
   f.dataset.otpVerified="true";
   msg.textContent=d.message||"Xác minh thành công.";msg.classList.add("show","success");
   btn.disabled=true;
   if(f.dataset.otpNext&&!f.parentNode.querySelector(`[href="${f.dataset.otpNext}"]`)){const link=document.createElement("a");link.href=f.dataset.otpNext;link.textContent="Tiếp tục";link.className="btn";link.style.display="block";msg.after(link)}
   return
  }
  if(f.dataset.endpoint==="/api/auth/reset-password")sessionStorage.removeItem("passwordResetToken");
  if(f.dataset.endpoint==="/api/auth/register"||f.dataset.endpoint==="/api/auth/staff/register"){
   const registration=collect(f),owner=f.dataset.endpoint.includes("/staff/")?registration.owner:registration;
   sessionStorage.setItem("otpEmailDestination",owner.email)
  }
  if(f.dataset.success){msg.textContent=f.dataset.success;msg.classList.add("show","success");if(f.dataset.next)setTimeout(()=>{location.href=f.dataset.next},1600);return}
  const next=d.redirect||f.dataset.next;if(next)location.href=next;else{msg.textContent=d.message||"Yêu cầu đã được xử lý.";msg.classList.add("show","success")}}
 catch(x){msg.textContent=x instanceof TypeError?"Không thể kết nối máy chủ. Vui lòng thử lại sau.":x.message;msg.classList.add("show")}
 finally{btn.removeAttribute("aria-busy");if(!f.dataset.otpVerified)btn.disabled=false;btn.textContent=originalText}});
const resetToken=new URLSearchParams(location.search).get("token")||sessionStorage.getItem("passwordResetToken");if(resetToken){const token=$("[name=token]");if(token)token.value=resetToken}
$$("[data-otp-prefill]").forEach(el=>{const key=el.dataset.otpPrefill==="email"?"otpEmailDestination":"otpPhoneDestination",value=sessionStorage.getItem(key);if(value)el.value=value});
const registered=new URLSearchParams(location.search).get("registered");
if(registered){const s=$(".sub");if(s)s.textContent=registered==="customer"?"Tài khoản đã được tạo. Bạn có thể đăng nhập.":"Đã gửi đăng ký cửa hàng. Tài khoản đang chờ quản trị viên xác minh."}
if(new URLSearchParams(location.search).get("reset")==="success"){const s=$(".sub");if(s)s.textContent="Mật khẩu đã được cập nhật. Hãy đăng nhập bằng mật khẩu mới."}
})();
