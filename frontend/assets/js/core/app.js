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
phoneLastFour:v=>/^\d{4}$/.test(v)?"":"Nhập đúng bốn chữ số.",
username:v=>!v||/^[A-Za-z0-9_.]{4,30}$/.test(v)?"":"Tên đăng nhập gồm 4–30 ký tự: chữ, số, _ hoặc .",
min8:v=>!v||v.length>=8?"":"Mật khẩu phải có ít nhất 8 ký tự"};
const emailValue=el=>{const domain=el.dataset.emailDomain&&document.getElementById(el.dataset.emailDomain);return domain?`${el.value.trim()}${domain.value}`:el.value};
const check=el=>{let m="";
 for(const r of (el.dataset.v||"").split(" ").filter(Boolean)){const[k,p]=r.split(":");
  if(k==="required")m=el.type==="checkbox"?(el.checked?"":el.dataset.requiredMessage||"Vui lòng xác nhận để tiếp tục"):V.required(el.value);
  else if(k==="match")m=el.value===el.form.elements[p].value?"":"Mật khẩu xác nhận không khớp";
  else m=k==="email"&&el.dataset.emailDomain?V.email(emailValue(el)):V[k](el.value);
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
document.addEventListener("change",e=>{if(e.target.type==="file"){const n=$(".fname",e.target.closest(".drop"));n.textContent=e.target.files[0]?e.target.files[0].name:"Chưa chọn tệp"}if(e.target.matches("[data-email-domain-select]"))$$("[data-email-domain]").filter(input=>input.dataset.emailDomain===e.target.id&&input.closest(".invalid")).forEach(check)});
document.addEventListener("click",e=>{
 const b=e.target.closest(".eye");
 if(b){const i=$("input",b.parentNode),show=i.type==="password";i.type=show?"text":"password";
  b.setAttribute("aria-label",show?"Ẩn mật khẩu":"Hiện mật khẩu");b.innerHTML=svg(show?"off":"eye")}
});

const collect=f=>{const o={};$$("[name]",f).forEach(el=>{
 if(el.type==="file"||el.dataset.skip!==undefined)return;
 const v=el.type==="checkbox"?el.checked:el.type==="password"?el.value:el.dataset.emailDomain?emailValue(el):el.value.trim();
 if(v===""&&!(el.dataset.v||"").includes("required"))return;
 el.name.split(".").reduce((a,k,i,r)=>i===r.length-1?(a[k]=v):(a[k]=a[k]||{}),o)});return o};
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
 btn.textContent=f.dataset.resetRequest?"Đang xác minh…":"Đang xử lý…";
 if(f.dataset.endpoint==="/api/auth/reset-password"&&!$("[name=token]",f).value){msg.textContent="Thông tin xác minh bị thiếu hoặc không hợp lệ. Vui lòng xác minh lại tài khoản.";msg.classList.add("show");btn.removeAttribute("aria-busy");btn.disabled=false;btn.textContent=originalText;return}
 try{const r=await fetch(API+f.dataset.endpoint,{method:"POST",credentials:"include",headers:{"Content-Type":"application/json","X-Requested-With":"fetch"},body:JSON.stringify(collect(f))});
  const d=await r.json().catch(()=>({}));
  if(!r.ok){
   if(r.status===401&&f.dataset.endpoint==="/api/users/profile"&&window.AppRoutes){
    const area=document.body.dataset.authArea||new URLSearchParams(location.search).get("area")||"customer";
    AppRoutes.handleSessionExpired(AppRoutes.roleForArea(area));return
   }
   throw new Error(d.message||({400:"Dữ liệu không hợp lệ.",429:"Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.",503:"Dịch vụ hiện chưa khả dụng. Vui lòng thử lại sau."}[r.status]||"Không thể hoàn tất yêu cầu. Vui lòng thử lại sau."))
  }
  if(f.dataset.loginRole){
   if(!d.success||!d.data||!Array.isArray(d.data.roles))throw new Error("Máy chủ không trả về thông tin vai trò hợp lệ.");
   const roles=d.data&&Array.isArray(d.data.roles)?d.data.roles:[];
   const requestedArea=new URLSearchParams(location.search).get("area");
   const requiredRole=requestedArea==="owner"?"OWNER":requestedArea==="staff"?"STAFF":f.dataset.loginRole;
   const allowed=requiredRole==="MANAGEMENT"?["OWNER","STAFF"]:[requiredRole];
   const role=allowed.find(candidate=>roles.includes(candidate));
   if(!role){
    const revoked=await fetch(API+"/api/auth/logout",{method:"POST",credentials:"include",headers:{"X-Requested-With":"fetch"}});
    if(!revoked.ok)throw new Error("Tài khoản không thuộc khu vực này và máy chủ chưa thể thu hồi phiên đăng nhập. Vui lòng đăng xuất rồi thử lại.");
    throw new Error("Tài khoản này không có quyền đăng nhập vào khu vực đã chọn.")
   }
   const returnUrl=new URLSearchParams(location.search).get("returnUrl");
   location.assign(AppRoutes.redirectByRole(role,returnUrl));return
  }
  if(f.dataset.resetRequest){
   const token=d.data&&d.data.resetToken;
   if(!token)throw new Error("Không thể xác minh thông tin. Vui lòng thử lại.");
   const resetPage=AppRoutes.getRoute(f.dataset.resetRoute);
   if(!resetPage)throw new Error("Đường dẫn đặt lại mật khẩu chưa được cấu hình.");
   sessionStorage.setItem("passwordResetToken",token);
   location.href=resetPage;
   return
  }
  if(f.dataset.endpoint==="/api/auth/reset-password")sessionStorage.removeItem("passwordResetToken");
  if(f.dataset.endpoint==="/api/users/profile"&&d.data){
   const fullName=$("#account-name"),phone=$("#account-phone");
   if(fullName)fullName.textContent=d.data.fullName||d.data.username||"";
   if(phone)phone.textContent=d.data.phone||"Chưa cung cấp";
   const lastFour=$("[name=phoneLastFour]",f);if(lastFour)lastFour.value=""
  }
  const nextRoute=f.dataset.nextRoute?AppRoutes.getRoute(f.dataset.nextRoute,Object.fromEntries(new URLSearchParams(f.dataset.nextQuery||""))):null;
  if(f.dataset.success){msg.textContent=f.dataset.success;msg.classList.add("show","success");if(nextRoute)setTimeout(()=>{location.href=nextRoute},1600);return}
  const next=d.redirect||nextRoute;if(next)location.href=next;else{msg.textContent=d.message||"Yêu cầu đã được xử lý.";msg.classList.add("show","success")}}
 catch(x){msg.textContent=x instanceof TypeError?"Không thể kết nối máy chủ. Vui lòng thử lại sau.":x.message;msg.classList.add("show")}
 finally{btn.removeAttribute("aria-busy");btn.disabled=false;btn.textContent=originalText}});
const resetToken=new URLSearchParams(location.search).get("token")||sessionStorage.getItem("passwordResetToken");if(resetToken){const token=$("[name=token]");if(token)token.value=resetToken}
const registered=new URLSearchParams(location.search).get("registered");
if(registered==="customer"){const s=$(".sub");if(s)s.textContent="Tài khoản đã được tạo. Bạn có thể đăng nhập."}
if(new URLSearchParams(location.search).get("reset")==="success"){const s=$(".sub");if(s)s.textContent="Mật khẩu đã được cập nhật. Hãy đăng nhập bằng mật khẩu mới."}
if(new URLSearchParams(location.search).get("expired")==="1"){const msg=$(".form-msg");if(msg){msg.textContent="Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";msg.classList.add("show")}}
})();
