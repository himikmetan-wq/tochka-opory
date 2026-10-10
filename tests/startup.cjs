const fs=require('fs'),vm=require('vm'),assert=require('assert');
let s=fs.readFileSync('index.html','utf8').match(/<script type="module">([\s\S]*?)<\/script>/)[1];
s=s.replace(/import\s+[\s\S]*?from\s+"[^"]+";/g,'').replace('boot();','globalThis.testApp={switchTab,state,boot,setUser:u=>activeUser=u,breakNotes:()=>{renderNotes=function(){throw new Error("Injected render failure");}}};');
const errors=[],authJobs=[];let splashFinished=0;
const nodes=new Map();const node=()=>({innerHTML:'',textContent:'',dataset:{},classList:{add(){},remove(){},toggle(){}},addEventListener(){},setAttribute(){},append(){},replaceChildren(){},remove(){}});
const document={documentElement:{dataset:{}},getElementById(id){if(!nodes.has(id))nodes.set(id,node());return nodes.get(id)},querySelector(){return node()},querySelectorAll(){return []},createElement:node,addEventListener(){}};
const c={console:{log:console.log,error:(...args)=>errors.push(args),warn(){}},document,Date,Math,Intl,Promise,Error,setTimeout,clearTimeout,setInterval:()=>1,clearInterval(){},requestAnimationFrame:f=>f(),localStorage:{getItem(){return '1'},setItem(){}},window:{matchMedia:()=>({matches:true}),addEventListener(){},scrollY:0,finishBootSplash(){splashFinished++;},setBootStatus(){}},initializeApp:()=>({}),initializeFirestore:()=>({}),getAuth:()=>({currentUser:{uid:'test'}}),onAuthStateChanged:(_,callback)=>authJobs.push(callback({uid:'test',displayName:'Test'})),doc:()=>({}),collection:()=>({}),query:x=>x,where:()=>({}),orderBy:()=>({}),limit:()=>({}),getDoc:async()=>({exists:()=>false}),getDocs:async()=>({docs:[]})};
vm.createContext(c);vm.runInContext(s,c);c.testApp.setUser({uid:'test'});
(async()=>{
  let release;
  const pending=new Promise(resolve=>{release=resolve;});
  c.getDoc=()=>pending;c.getDocs=()=>pending;
  c.testApp.boot();
  assert(splashFinished>0,'Splash must finish before cloud reads return');
  assert(nodes.get('main-content').innerHTML.includes('Наставник'),'Shell must render before cloud reads return');
  release({exists:()=>false,docs:[]});
  await Promise.all(authJobs);
  assert(nodes.get('main-content').innerHTML.includes('Заметки'),'Authenticated startup must display notes');
  assert.equal(errors.length,0,'Startup must not log runtime errors');
  for(const tab of ['journal','tasks','tracker','finance','budget','summary']){
    await c.testApp.switchTab(tab);
    assert(nodes.get('main-content').innerHTML.length>0,tab+' must render');
    assert.equal(errors.length,0,tab+' must not log runtime errors');
  }
  c.testApp.breakNotes();
  await c.testApp.switchTab('summary');
  const html=nodes.get('main-content').innerHTML;
  assert(html.includes('Injected render failure'),'Failed block must show an actionable error');
  assert(html.includes('Наставник')&&html.includes('Блок чемпиона'),'Other blocks must remain visible');
  await c.testApp.switchTab('tasks');
  assert(!nodes.get('main-content').innerHTML.includes('Injected render failure'),'Navigation must work after a block fails');
  console.log('PASS: authenticated startup, all six tabs, isolated render failure and recovery');
})().catch(e=>{console.error(e);process.exitCode=1});
