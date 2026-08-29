"""Cached BASE/ES-boost experiment runner; no model promotion."""
import csv, hashlib, json, time
from pathlib import Path
import numpy as np
import train

def load(path, triple=False):
    with open(path, encoding='utf-8', newline='') as s:
        return [(r['title'].strip(), r['language'].strip()) for r in csv.DictReader(s)]

def distinguishable(title):
    words=set(title.casefold().split())
    shared={'amor','calma','vida','solo','bella','forma','momento','dado','dada','mano'}
    return not (len(words)<=1 and words & shared or len(words)<=2 and words & shared)

def directional(rows, weights, bias, expected, predicted):
    selected=[r for r in rows if r[1]==expected and distinguishable(r[0])]
    errors=sum(train.classify_prediction(title,weights,bias)[0]==predicted for title,_ in selected)
    return errors/len(selected) if selected else 0.0

def main():
    root=Path(__file__).resolve().parent; out=root/'build-cached-boosts'; out.mkdir(exist_ok=True)
    cache=out/'matrix-cache.npz'; meta=out/'cache-meta.json'; started=time.perf_counter()
    base=load(root/'datasets/openlid-four-language.csv'); domain=load(root/'datasets/music_titles.csv'); music=load(root/'data/music-real.csv')
    bt,val,test=train.split(base); dt,_,_=train.split(domain); mt,_,_=train.split(music)
    rows=bt+dt+mt; key=hashlib.sha256(('sparse-v2'+''.join(t+'\0'+l for t,l in rows)+str(train.FEATURES)+str(train.SEED)).encode()).hexdigest()
    from scipy.sparse import save_npz, load_npz
    if cache.exists() and meta.exists() and json.loads(meta.read_text())['key']==key:
        X=load_npz(cache); y=np.load(out/'labels.npy'); vector_seconds=0
    else:
        X,y=train.matrix_for(rows); save_npz(cache,X); np.save(out/'labels.npy',y); meta.write_text(json.dumps({'key':key,'features':train.FEATURES,'seed':train.SEED})); vector_seconds=time.perf_counter()-started
    from sklearn.linear_model import SGDClassifier
    golden_path=root.parent.parent/'src'/'test'/'resources'/'language'/'golden-dataset.csv'
    golden=[]
    if golden_path.exists():
        with golden_path.open(encoding='utf-8',newline='') as s:
            golden=[(r['title'].strip(),r['language'].strip(),r.get('ambiguous_acceptable','0')=='1') for r in csv.DictReader(s)]
    results=[]
    for boost in (1.0,1.25,1.5):
        weights=np.ones(len(rows)); offset=len(bt)
        for i,(title,lang) in enumerate(dt,offset):
            weights[i]=5.0 if lang in ('es','pt') and len(title.split())<=3 else 3.0
        offset += len(dt)
        for i,(title,lang) in enumerate(mt,offset):
            weights[i]=5.0*(1.5 if lang in ('es','pt') and len(title.split())<=3 else 1.0)
            if lang=='es': weights[i]*=boost
        c=SGDClassifier(loss='log_loss',alpha=1e-6,max_iter=80,tol=1e-4,random_state=train.SEED,average=True).fit(X,y,sample_weight=weights)
        w,b=c.coef_.T.astype(float),c.intercept_.astype(float); metrics=train.evaluate(test,w,b); g=out/f'boost-{boost:g}'; g.mkdir(exist_ok=True); train.write_es_pt_errors(test,w,b,g/'test-es-to-pt-errors.csv')
        gd=train.diagnose_golden(golden,w,b,g/'golden-diagnostics.csv') if golden else {}
        results.append({'boost':boost,'test_accuracy':metrics['accuracy'],'macro_f1':metrics['macro_f1'],'es_to_pt':metrics['es_to_pt_false_positive_rate'],'pt_to_es':metrics['pt_to_es_false_positive_rate'],'es_to_pt_distinguishable':directional(test,w,b,'es','pt'),'pt_to_es_distinguishable':directional(test,w,b,'pt','es'),'accuracy_by_language':metrics['accuracy_by_language'],'one_word':metrics['by_length'].get('1_word',{}).get('accuracy',0),'two_words':metrics['by_length'].get('2_words',{}).get('accuracy',0),'three_to_five':metrics['by_length'].get('3_5_words',{}).get('accuracy',0),'six_plus':metrics['by_length'].get('6_10_words',{}).get('accuracy',0),'golden_exact':gd.get('exact_accuracy',0),'golden_accepted':gd.get('accepted_accuracy',0),'golden_coverage':gd.get('coverage',0),'definitive_accuracy':gd.get('accuracy_on_definitive',0),'confident_errors':gd.get('confident_wrong_predictions',0)})
    json.dump({'vectorization_seconds':vector_seconds,'total_seconds':time.perf_counter()-started,'results':results},(out/'comparison.json').open('w',encoding='utf-8'),indent=2)
    print(json.dumps({'vectorization_seconds':vector_seconds,'results':results},indent=2))
if __name__=='__main__': main()
