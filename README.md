# KGRC-CSV2RDF
ナレッジグラフ推論チャレンジ用の「ナレッジグラフ」変換用プログラム

## 使い方

### 小説間の語彙統制用の前処理
1. data/KGRC2019/org に元データ（TSV）ファイルを置く
2. data/KGRC2019/setting に語彙統一用の辞書データ（PredList.tsv）を置く
3. PreProcTSVforKG.classを実行
4. data/KGRC2019 に語彙統一されたTSVファイルが出力される

### TSVからRDF（Turtle）への変換
1. data/KGRC2019 に元データ（TSV）ファイルを置く
2. TSV2RDFforKGRC2019.classを実行
3. data/KGRC2019 に変換されたRDF(Turtle)ファイルが出力される


